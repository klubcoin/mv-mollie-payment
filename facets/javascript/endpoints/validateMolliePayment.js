const validateMolliePayment = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/validateMolliePayment/${parameters.orderId}`, baseUrl);
	return fetch(url.toString(), {
		method: 'GET'
	});
}

const validateMolliePaymentForm = (container) => {
	const html = `<form id='validateMolliePayment-form'>
		<div id='validateMolliePayment-orderId-form-field'>
			<label for='orderId'>orderId</label>
			<input type='text' id='validateMolliePayment-orderId-param' name='orderId'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const orderId = container.querySelector('#validateMolliePayment-orderId-param');

	container.querySelector('#validateMolliePayment-form button').onclick = () => {
		const params = {
			orderId : orderId.value !== "" ? orderId.value : undefined
		};

		validateMolliePayment(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { validateMolliePayment, validateMolliePaymentForm };