const moUpdateVendor = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/moUpdateVendor/${parameters.uuid}`, baseUrl);
	return fetch(url.toString(), {
		method: 'PUT'
	});
}

const moUpdateVendorForm = (container) => {
	const html = `<form id='moUpdateVendor-form'>
		<div id='moUpdateVendor-uuid-form-field'>
			<label for='uuid'>uuid</label>
			<input type='text' id='moUpdateVendor-uuid-param' name='uuid'/>
		</div>
		<div id='moUpdateVendor-domain-form-field'>
			<label for='domain'>domain</label>
			<input type='text' id='moUpdateVendor-domain-param' name='domain'/>
		</div>
		<div id='moUpdateVendor-walletAddress-form-field'>
			<label for='walletAddress'>walletAddress</label>
			<input type='text' id='moUpdateVendor-walletAddress-param' name='walletAddress'/>
		</div>
		<div id='moUpdateVendor-method-form-field'>
			<label for='method'>method</label>
			<input type='text' id='moUpdateVendor-method-param' name='method'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const uuid = container.querySelector('#moUpdateVendor-uuid-param');
	const domain = container.querySelector('#moUpdateVendor-domain-param');
	const walletAddress = container.querySelector('#moUpdateVendor-walletAddress-param');
	const method = container.querySelector('#moUpdateVendor-method-param');

	container.querySelector('#moUpdateVendor-form button').onclick = () => {
		const params = {
			uuid : uuid.value !== "" ? uuid.value : undefined,
			domain : domain.value !== "" ? domain.value : undefined,
			walletAddress : walletAddress.value !== "" ? walletAddress.value : undefined,
			method : method.value !== "" ? method.value : undefined
		};

		moUpdateVendor(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { moUpdateVendor, moUpdateVendorForm };