const moCreateVendor = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/moCreateVendor/`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			domain : parameters.domain,
			walletAddress : parameters.walletAddress,
			method : parameters.method
		})
	});
}

const moCreateVendorForm = (container) => {
	const html = `<form id='moCreateVendor-form'>
		<div id='moCreateVendor-domain-form-field'>
			<label for='domain'>domain</label>
			<input type='text' id='moCreateVendor-domain-param' name='domain'/>
		</div>
		<div id='moCreateVendor-walletAddress-form-field'>
			<label for='walletAddress'>walletAddress</label>
			<input type='text' id='moCreateVendor-walletAddress-param' name='walletAddress'/>
		</div>
		<div id='moCreateVendor-method-form-field'>
			<label for='method'>method</label>
			<input type='text' id='moCreateVendor-method-param' name='method'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const domain = container.querySelector('#moCreateVendor-domain-param');
	const walletAddress = container.querySelector('#moCreateVendor-walletAddress-param');
	const method = container.querySelector('#moCreateVendor-method-param');

	container.querySelector('#moCreateVendor-form button').onclick = () => {
		const params = {
			domain : domain.value !== "" ? domain.value : undefined,
			walletAddress : walletAddress.value !== "" ? walletAddress.value : undefined,
			method : method.value !== "" ? method.value : undefined
		};

		moCreateVendor(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { moCreateVendor, moCreateVendorForm };