const moGetPayment = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/moGetPayment/${parameters.paymentId}`, baseUrl);
	return fetch(url.toString(), {
		method: 'GET'
	});
}

const moGetPaymentForm = (container) => {
	const html = `<form id='moGetPayment-form'>
		<div id='moGetPayment-paymentId-form-field'>
			<label for='paymentId'>paymentId</label>
			<input type='text' id='moGetPayment-paymentId-param' name='paymentId'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const paymentId = container.querySelector('#moGetPayment-paymentId-param');

	container.querySelector('#moGetPayment-form button').onclick = () => {
		const params = {
			paymentId : paymentId.value !== "" ? paymentId.value : undefined
		};

		moGetPayment(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { moGetPayment, moGetPaymentForm };