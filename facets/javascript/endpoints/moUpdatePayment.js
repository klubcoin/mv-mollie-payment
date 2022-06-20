const moUpdatePayment = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/moUpdatePayment/${parameters.paymentId}`, baseUrl);
	return fetch(url.toString(), {
		method: 'PUT'
	});
}

const moUpdatePaymentForm = (container) => {
	const html = `<form id='moUpdatePayment-form'>
		<div id='moUpdatePayment-paymentId-form-field'>
			<label for='paymentId'>paymentId</label>
			<input type='text' id='moUpdatePayment-paymentId-param' name='paymentId'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const paymentId = container.querySelector('#moUpdatePayment-paymentId-param');

	container.querySelector('#moUpdatePayment-form button').onclick = () => {
		const params = {
			paymentId : paymentId.value !== "" ? paymentId.value : undefined
		};

		moUpdatePayment(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { moUpdatePayment, moUpdatePaymentForm };