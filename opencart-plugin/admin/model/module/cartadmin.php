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

		$this->db->query("CREATE TABLE IF NOT EXISTS `" . DB_PREFIX . "cartadmin_token` (
			`token_id` INT AUTO_INCREMENT PRIMARY KEY,
			`token_lookup` CHAR(16) NULL,
			`token_hash` VARCHAR(255) NOT NULL,
			`last_four` CHAR(4) NOT NULL,
			`label` VARCHAR(64) NOT NULL,
			`operator_user_id` INT NULL,
			`operator_name` VARCHAR(64) NOT NULL,
			`scopes` VARCHAR(255) NOT NULL,
			`device_hash` CHAR(64) NULL,
			`device_public_key` TEXT NULL,
			`active` TINYINT(1) NOT NULL DEFAULT 1,
			`created_at` DATETIME NOT NULL,
			`last_used_at` DATETIME NULL,
			`revoked_at` DATETIME NULL,
			UNIQUE KEY `uq_token_lookup` (`token_lookup`),
			INDEX `idx_active` (`active`, `token_id`)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

		$this->db->query("CREATE TABLE IF NOT EXISTS `" . DB_PREFIX . "cartadmin_security_audit` (
			`audit_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
			`event_id` VARCHAR(64) NOT NULL,
			`token_id` INT NULL,
			`operator_user_id` INT NULL,
			`operator_name` VARCHAR(64) NOT NULL,
			`claimed_operator_digest` CHAR(64) NOT NULL DEFAULT '',
			`identity_claim_mismatch` TINYINT(1) NOT NULL DEFAULT 0,
			`device_hash` CHAR(64) NOT NULL DEFAULT '',
			`ip_hash` CHAR(64) NOT NULL DEFAULT '',
			`action_name` VARCHAR(64) NOT NULL,
			`module_name` VARCHAR(32) NOT NULL DEFAULT '',
			`target_id` INT NOT NULL DEFAULT 0,
			`result` VARCHAR(16) NOT NULL,
			`before_digest` CHAR(64) NOT NULL DEFAULT '',
			`after_digest` CHAR(64) NOT NULL DEFAULT '',
			`summary` VARCHAR(255) NOT NULL DEFAULT '',
			`created_at` DATETIME NOT NULL,
			UNIQUE KEY `uq_event_id` (`event_id`),
			INDEX `idx_security_created` (`created_at`),
			INDEX `idx_security_token` (`token_id`, `created_at`)
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
		$device_key_column = $this->db->query("SHOW COLUMNS FROM `" . DB_PREFIX . "cartadmin_token` LIKE 'device_public_key'");
		if (!$device_key_column->num_rows) {
			$this->db->query("ALTER TABLE `" . DB_PREFIX . "cartadmin_token` ADD COLUMN `device_public_key` TEXT NULL AFTER `device_hash`");
		}

		$this->db->query("CREATE TABLE IF NOT EXISTS `" . DB_PREFIX . "cartadmin_rate_limit` (
			`rate_key` CHAR(64) PRIMARY KEY,
			`failures` SMALLINT UNSIGNED NOT NULL DEFAULT '0',
			`window_started` DATETIME NOT NULL,
			`blocked_until` DATETIME NULL,
			INDEX `idx_rate_cleanup` (`window_started`, `blocked_until`)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

		$this->db->query("CREATE TABLE IF NOT EXISTS `" . DB_PREFIX . "cartadmin_device_nonce` (
			`token_id` INT NOT NULL,
			`nonce` CHAR(36) NOT NULL,
			`created_at` DATETIME NOT NULL,
			PRIMARY KEY (`token_id`, `nonce`),
			INDEX `idx_nonce_cleanup` (`created_at`)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

		$this->migrateLegacyToken();
	}

	public function createToken(string $label, int $operator_user_id, array $scopes): string {
		$allowedScopes = ['status.read', 'orders.read', 'catalog.read', 'content.read', 'customers.read', 'telemetry.read', 'orders.write', 'catalog.write', 'content.write', 'customers.write'];
		$cleanScopes = array_values(array_intersect($allowedScopes, array_unique(array_map('strval', $scopes))));
		$label = trim(strip_tags($label));

		if ($label === '' || mb_strlen($label) > 64 || $operator_user_id < 1 || !in_array('status.read', $cleanScopes, true)) {
			throw new \InvalidArgumentException('Dati del token CartAdmin non validi.');
		}
		$operatorQuery = $this->db->query("SELECT `user_id`, `username` FROM `" . DB_PREFIX . "user` WHERE `user_id` = '" . (int)$operator_user_id . "' AND `status` = '1' LIMIT 1");
		if (!$operatorQuery->num_rows) {
			throw new \InvalidArgumentException('Operatore OpenCart non valido o disabilitato.');
		}
		$operator_name = mb_substr(trim(strip_tags((string)$operatorQuery->row['username'])), 0, 64);
		if ($operator_name === '') {
			throw new \InvalidArgumentException('Nome operatore OpenCart non valido.');
		}

		$count = $this->db->query("SELECT COUNT(*) AS `total` FROM `" . DB_PREFIX . "cartadmin_token` WHERE `active` = '1'");
		if ((int)($count->row['total'] ?? 0) >= 50) {
			throw new \RuntimeException('Limite massimo di token attivi raggiunto.');
		}

		$lookup = bin2hex(random_bytes(8));
		$token = 'ca_' . $lookup . '_' . bin2hex(random_bytes(32));
		$hash = $this->hashToken($token);
		$scopeCsv = implode(',', $cleanScopes);

		$this->db->query("START TRANSACTION");

		try {
			$this->db->query("INSERT INTO `" . DB_PREFIX . "cartadmin_token` SET `token_lookup` = '" . $this->db->escape($lookup) . "', `token_hash` = '" . $this->db->escape($hash) . "', `last_four` = '" . $this->db->escape(substr($token, -4)) . "', `label` = '" . $this->db->escape($label) . "', `operator_user_id` = '" . (int)$operator_user_id . "', `operator_name` = '" . $this->db->escape($operator_name) . "', `scopes` = '" . $this->db->escape($scopeCsv) . "', `active` = '1', `created_at` = NOW()");
			$this->db->query("COMMIT");
		} catch (\Throwable $exception) {
			$this->db->query("ROLLBACK");
			throw $exception;
		}

		return $token;
	}

	public function getTokenState(): array {
		$query = $this->db->query("SELECT `last_four`, `created_at` FROM `" . DB_PREFIX . "cartadmin_token` WHERE `active` = '1' ORDER BY `token_id` DESC LIMIT 1");
		$count = $this->db->query("SELECT COUNT(*) AS `total` FROM `" . DB_PREFIX . "cartadmin_token` WHERE `active` = '1'");

		return [
			'configured' => (int)($count->row['total'] ?? 0) > 0,
			'active_count' => (int)($count->row['total'] ?? 0),
			'last_four'  => $query->num_rows ? (string)$query->row['last_four'] : '',
			'created_at' => $query->num_rows ? (string)$query->row['created_at'] : ''
		];
	}

	public function getTokens(): array {
		$query = $this->db->query("SELECT `token_id`, `last_four`, `label`, `operator_user_id`, `operator_name`, `scopes`, `device_hash`, `active`, `created_at`, `last_used_at`, `revoked_at` FROM `" . DB_PREFIX . "cartadmin_token` ORDER BY `active` DESC, `token_id` DESC LIMIT 100");

		return $query->rows;
	}

	public function getOperators(): array {
		$query = $this->db->query("SELECT `user_id`, `username`, `firstname`, `lastname` FROM `" . DB_PREFIX . "user` WHERE `status` = '1' ORDER BY `username` ASC");

		return $query->rows;
	}

	public function revokeToken(int $token_id): bool {
		if ($token_id < 1) {
			return false;
		}
		$this->db->query("UPDATE `" . DB_PREFIX . "cartadmin_token` SET `active` = '0', `revoked_at` = NOW() WHERE `token_id` = '" . (int)$token_id . "' AND `active` = '1'");

		return $this->db->countAffected() === 1;
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
		$legacy = trim($this->getValue('api_key'));
		$lastFour = $this->getValue('token_last_four');
		$createdAt = $this->getValue('token_created_at');
		if ($this->getValue('legacy_token_migrated') === '1' && $hash === '' && $legacy === '') {
			return;
		}

		if ($hash === '' && $legacy !== '') {
			$hash = $this->hashToken($legacy);
			$lastFour = substr($legacy, -4);
		}

		$this->db->query("START TRANSACTION");
		try {
			if ($hash !== '') {
				$createdSql = $createdAt !== '' ? "'" . $this->db->escape($createdAt) . "'" : 'NOW()';
				$this->db->query("INSERT INTO `" . DB_PREFIX . "cartadmin_token` SET `token_hash` = '" . $this->db->escape($hash) . "', `last_four` = '" . $this->db->escape(substr($lastFour, -4)) . "', `label` = 'Token legacy revocato', `operator_name` = 'Operatore legacy', `scopes` = '', `active` = '0', `created_at` = " . $createdSql . ", `revoked_at` = NOW()");
			}
			$this->upsert('legacy_token_migrated', '1');
			$this->db->query("DELETE FROM `" . DB_PREFIX . "cartadmin_setting` WHERE `key` IN ('api_key', 'token_hash', 'token_last_four', 'token_created_at')");
			$this->db->query("COMMIT");
		} catch (\Throwable $exception) {
			$this->db->query("ROLLBACK");
			throw $exception;
		}
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
