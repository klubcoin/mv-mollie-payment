const moPaymentStatus = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/moPaymentStatus/${parameters.orderId}`, baseUrl);
	return fetch(url.toString(), {
		method: 'GET'
	});
}

const moPaymentStatusForm = (container) => {
	const html = `<form id='moPaymentStatus-form'>
		<div id='moPaymentStatus-orderId-form-field'>
			<label for='orderId'>orderId</label>
			<input type='text' id='moPaymentStatus-orderId-param' name='orderId'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const orderId = container.querySelector('#moPaymentStatus-orderId-param');

	container.querySelector('#moPaymentStatus-form button').onclick = () => {
		const params = {
			orderId : orderId.value !== "" ? orderId.value : undefined
		};

		moPaymentStatus(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { moPaymentStatus, moPaymentStatusForm };