const moPaymentCheckoutPage = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/moPaymentCheckoutPage/${parameters.orderId}`, baseUrl);
	return fetch(url.toString(), {
		method: 'GET'
	});
}

const moPaymentCheckoutPageForm = (container) => {
	const html = `<form id='moPaymentCheckoutPage-form'>
		<div id='moPaymentCheckoutPage-orderId-form-field'>
			<label for='orderId'>orderId</label>
			<input type='text' id='moPaymentCheckoutPage-orderId-param' name='orderId'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const orderId = container.querySelector('#moPaymentCheckoutPage-orderId-param');

	container.querySelector('#moPaymentCheckoutPage-form button').onclick = () => {
		const params = {
			orderId : orderId.value !== "" ? orderId.value : undefined
		};

		moPaymentCheckoutPage(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { moPaymentCheckoutPage, moPaymentCheckoutPageForm };