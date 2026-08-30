<?php
namespace Opencart\Admin\Model\Extension\Cartadmin\Module;

class Cartadmin extends \Opencart\System\Engine\Model {
	public function install(): void {
		$this->db->query("CREATE TABLE IF NOT EXISTS `" . DB_PREFIX . "cartadmin_setting` (
			`key` VARCHAR(64) NOT NULL PRIMARY KEY,
			`value` TEXT NOT NULL,
			`date_updated` DATETIME NOT NULL
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

		$this->db->query("CREATE TABLE IF NOT EXISTS `" . DB_PREFIX . "cartadmin_audit` (
			`id` INT AUTO_INCREMENT PRIMARY KEY,
			`log_id` VARCHAR(64) NOT NULL,
			`action_type` VARCHAR(64) NOT NULL,
			`description` VARCHAR(255) NOT NULL,
			`operator_username` VARCHAR(64) NOT NULL,
			`timestamp_iso` VARCHAR(64) NOT NULL,
			`device_model` VARCHAR(128) NOT NULL,
			`android_version` VARCHAR(64) NOT NULL,
			`app_version` VARCHAR(32) NOT NULL,
			`created_at` DATETIME NOT NULL,
			INDEX `idx_timestamp` (`created_at`)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

		$this->db->query("CREATE TABLE IF NOT EXISTS `" . DB_PREFIX . "cartadmin_command` (
			`command_id` INT AUTO_INCREMENT PRIMARY KEY,
			`module` VARCHAR(32) NOT NULL,
			`target_id` INT NOT NULL,
			`operation` VARCHAR(16) NOT NULL,
			`requested_by` VARCHAR(64) NOT NULL,
			`status` VARCHAR(16) NOT NULL DEFAULT 'pending',
			`dedupe_key` VARCHAR(80) NULL,
			`error_message` VARCHAR(255) NOT NULL DEFAULT '',
			`created_at` DATETIME NOT NULL,
			`processed_at` DATETIME NULL,
			`processed_by` INT NULL,
			UNIQUE KEY `uq_pending_target` (`dedupe_key`),
			INDEX `idx_command_status` (`status`, `created_at`)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

		$this->migrateLegacyToken();
	}

	public function rotateToken(): string {
		$token = 'ca_' . bin2hex(random_bytes(32));
		$hash = $this->hashToken($token);

		$this->db->query("START TRANSACTION");

		try {
			$this->upsert('token_hash', $hash);
			$this->upsert('token_last_four', substr($token, -4));
			$this->upsert('token_created_at', date('Y-m-d H:i:s'));
			$this->db->query("DELETE FROM `" . DB_PREFIX . "cartadmin_setting` WHERE `key` = 'api_key'");
			$this->db->query("COMMIT");
		} catch (\Throwable $exception) {
			$this->db->query("ROLLBACK");
			throw $exception;
		}

		return $token;
	}

	public function getTokenState(): array {
		$values = [];
		$query = $this->db->query("SELECT `key`, `value` FROM `" . DB_PREFIX . "cartadmin_setting` WHERE `key` IN ('token_hash', 'token_last_four', 'token_created_at')");

		foreach ($query->rows as $row) {
			$values[$row['key']] = $row['value'];
		}

		return [
			'configured' => !empty($values['token_hash']),
			'last_four'  => $values['token_last_four'] ?? '',
			'created_at' => $values['token_created_at'] ?? ''
		];
	}

	public function getPendingCommands(): array {
		$query = $this->db->query("SELECT `command_id`, `module`, `target_id`, `operation`, `requested_by`, `created_at` FROM `" . DB_PREFIX . "cartadmin_command` WHERE `status` = 'pending' ORDER BY `created_at` ASC, `command_id` ASC");

		return $query->rows;
	}

	/**
	 * Esegue una richiesta mobile soltanto nel contesto amministrativo nativo.
	 * Le chiamate ai model passano dal Loader OpenCart e attivano quindi gli
	 * eventi ufficiali (incluse le email cliente/GDPR configurate dallo store).
	 */
	public function processCommand(int $command_id, string $decision, int $user_id): void {
		if (!in_array($decision, ['execute', 'reject'], true)) {
			throw new \InvalidArgumentException('Decisione comando non valida.');
		}

		$query = $this->db->query("SELECT * FROM `" . DB_PREFIX . "cartadmin_command` WHERE `command_id` = '" . (int)$command_id . "' AND `status` = 'pending' LIMIT 1");
		if (!$query->num_rows) {
			throw new \OutOfBoundsException('Comando non trovato o già elaborato.');
		}
		$command = $query->row;

		if ($decision === 'reject') {
			$this->db->query("UPDATE `" . DB_PREFIX . "cartadmin_command` SET `status` = 'rejected', `dedupe_key` = NULL, `processed_at` = NOW(), `processed_by` = '" . (int)$user_id . "' WHERE `command_id` = '" . (int)$command_id . "' AND `status` = 'pending'");
			return;
		}

		$this->db->query("UPDATE `" . DB_PREFIX . "cartadmin_command` SET `status` = 'processing', `processed_by` = '" . (int)$user_id . "' WHERE `command_id` = '" . (int)$command_id . "' AND `status` = 'pending'");
		if ($this->db->countAffected() !== 1) {
			throw new \RuntimeException('Il comando è già in elaborazione.');
		}

		try {
			$target_id = (int)$command['target_id'];
			$operation = (string)$command['operation'];
			if (!in_array($operation, ['approve', 'deny'], true)) {
				throw new \RuntimeException('Operazione comando non supportata.');
			}

			if ($command['module'] === 'customer_approvals') {
				$this->load->model('customer/customer_approval');
				$approval = $this->model_customer_customer_approval->getCustomerApproval($target_id);
				if (!$approval) {
					throw new \OutOfBoundsException('Richiesta cliente non più disponibile.');
				}

				if ($approval['type'] === 'customer') {
					$operation === 'approve'
						? $this->model_customer_customer_approval->approveCustomer((int)$approval['customer_id'])
						: $this->model_customer_customer_approval->denyCustomer((int)$approval['customer_id']);
				} elseif ($approval['type'] === 'affiliate') {
					$operation === 'approve'
						? $this->model_customer_customer_approval->approveAffiliate((int)$approval['customer_id'])
						: $this->model_customer_customer_approval->denyAffiliate((int)$approval['customer_id']);
				} else {
					throw new \RuntimeException('Tipo approvazione cliente non supportato.');
				}
			} elseif ($command['module'] === 'gdpr') {
				$this->load->model('customer/gdpr');
				$gdpr = $this->model_customer_gdpr->getGdpr($target_id);
				if (!$gdpr || (int)$gdpr['status'] !== 1) {
					throw new \OutOfBoundsException('Richiesta GDPR non più in attesa.');
				}
				$status = $operation === 'deny' ? -1 : ((string)$gdpr['action'] === 'export' ? 3 : 2);
				$this->model_customer_gdpr->editStatus($target_id, $status);
			} else {
				throw new \RuntimeException('Modulo comando non supportato.');
			}

			$this->db->query("UPDATE `" . DB_PREFIX . "cartadmin_command` SET `status` = 'completed', `dedupe_key` = NULL, `processed_at` = NOW(), `error_message` = '' WHERE `command_id` = '" . (int)$command_id . "'");
		} catch (\Throwable $exception) {
			$this->db->query("UPDATE `" . DB_PREFIX . "cartadmin_command` SET `status` = 'failed', `dedupe_key` = NULL, `processed_at` = NOW(), `error_message` = 'Esecuzione nativa OpenCart non riuscita.' WHERE `command_id` = '" . (int)$command_id . "'");
			throw $exception;
		}
	}

	private function migrateLegacyToken(): void {
		$hash = $this->getValue('token_hash');

		if ($hash !== '') {
			return;
		}

		$legacy = trim($this->getValue('api_key'));

		if ($legacy === '') {
			return;
		}

		$this->upsert('token_hash', $this->hashToken($legacy));
		$this->upsert('token_last_four', substr($legacy, -4));
		$this->upsert('token_created_at', date('Y-m-d H:i:s'));
		$this->db->query("DELETE FROM `" . DB_PREFIX . "cartadmin_setting` WHERE `key` = 'api_key'");
	}

	private function hashToken(string $token): string {
		$algorithm = defined('PASSWORD_ARGON2ID') ? PASSWORD_ARGON2ID : PASSWORD_DEFAULT;
		$hash = password_hash($token, $algorithm);

		if (!is_string($hash) || $hash === '') {
			throw new \RuntimeException('Impossibile proteggere il token CartAdmin.');
		}

		return $hash;
	}

	private function getValue(string $key): string {
		$query = $this->db->query("SELECT `value` FROM `" . DB_PREFIX . "cartadmin_setting` WHERE `key` = '" . $this->db->escape($key) . "' LIMIT 1");

		return $query->num_rows ? (string)$query->row['value'] : '';
	}

	private function upsert(string $key, string $value): void {
		$this->db->query("INSERT INTO `" . DB_PREFIX . "cartadmin_setting` SET `key` = '" . $this->db->escape($key) . "', `value` = '" . $this->db->escape($value) . "', `date_updated` = NOW() ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), `date_updated` = NOW()");
	}
}
