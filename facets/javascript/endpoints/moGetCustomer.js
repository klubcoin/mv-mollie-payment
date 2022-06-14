const moGetCustomer = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/moGetCustomer/${parameters.customerId}`, baseUrl);
	return fetch(url.toString(), {
		method: 'GET'
	});
}

const moGetCustomerForm = (container) => {
	const html = `<form id='moGetCustomer-form'>
		<div id='moGetCustomer-customerId-form-field'>
			<label for='customerId'>customerId</label>
			<input type='text' id='moGetCustomer-customerId-param' name='customerId'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const customerId = container.querySelector('#moGetCustomer-customerId-param');

	container.querySelector('#moGetCustomer-form button').onclick = () => {
		const params = {
			customerId : customerId.value !== "" ? customerId.value : undefined
		};

		moGetCustomer(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { moGetCustomer, moGetCustomerForm };