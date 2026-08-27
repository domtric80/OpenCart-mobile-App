<?php
namespace Opencart\Admin\Controller\Extension\Cartadmin\Module;

class Cartadmin extends \Opencart\System\Engine\Controller {
	private string $route = 'extension/cartadmin/module/cartadmin';

	public function index(): void {
		$this->load->language($this->route);
		$this->document->setTitle($this->language->get('heading_title'));

		$token = 'user_token=' . $this->session->data['user_token'];
		$data['breadcrumbs'] = [
			[
				'text' => $this->language->get('text_home'),
				'href' => $this->url->link('common/dashboard', $token)
			],
			[
				'text' => $this->language->get('text_extension'),
				'href' => $this->url->link('marketplace/extension', $token . '&type=module')
			],
			[
				'text' => $this->language->get('heading_title'),
				'href' => $this->url->link($this->route, $token)
			]
		];

		$this->load->model('extension/cartadmin/module/cartadmin');
		$this->model_extension_cartadmin_module_cartadmin->install();
		$state = $this->model_extension_cartadmin_module_cartadmin->getTokenState();

		$data['configured'] = $state['configured'];
		$data['last_four'] = $state['last_four'];
		$data['created_at'] = $state['created_at'];
		$data['generate'] = $this->url->link($this->route . '.generate', $token, true);
		$data['back'] = $this->url->link('marketplace/extension', $token . '&type=module');
		$data['endpoint'] = rtrim(HTTP_CATALOG, '/') . '/extension/cartadmin/cartadmin_api.php';
		$data['header'] = $this->load->controller('common/header');
		$data['column_left'] = $this->load->controller('common/column_left');
		$data['footer'] = $this->load->controller('common/footer');

		$this->response->setOutput($this->load->view($this->route, $data));
	}

	public function generate(): void {
		$this->load->language($this->route);
		$json = [];

		if (!$this->user->hasPermission('modify', $this->route)) {
			$json['error'] = $this->language->get('error_permission');
		} elseif (($this->request->server['REQUEST_METHOD'] ?? '') !== 'POST') {
			$json['error'] = $this->language->get('error_method');
		}

		if (!$json) {
			$this->load->model('extension/cartadmin/module/cartadmin');
			$this->model_extension_cartadmin_module_cartadmin->install();
			$token = $this->model_extension_cartadmin_module_cartadmin->rotateToken();

			$json['success'] = $this->language->get('text_token_generated');
			$json['token'] = $token;
			$json['last_four'] = substr($token, -4);
		}

		$this->response->addHeader('Content-Type: application/json');
		$this->response->addHeader('Cache-Control: no-store');
		$this->response->addHeader('X-Content-Type-Options: nosniff');
		$this->response->setOutput(json_encode($json, JSON_UNESCAPED_SLASHES));
	}

	public function install(): void {
		$this->load->model('extension/cartadmin/module/cartadmin');
		$this->model_extension_cartadmin_module_cartadmin->install();
	}

	public function uninstall(): void {
		// Token e audit restano nel database per evitare perdita accidentale di dati.
	}
}
