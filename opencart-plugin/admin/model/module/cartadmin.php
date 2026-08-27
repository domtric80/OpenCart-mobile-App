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
