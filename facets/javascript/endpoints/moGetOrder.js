const moGetOrder = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/moGetOrder/${parameters.orderId}`, baseUrl);
	return fetch(url.toString(), {
		method: 'GET'
	});
}

const moGetOrderForm = (container) => {
	const html = `<form id='moGetOrder-form'>
		<div id='moGetOrder-orderId-form-field'>
			<label for='orderId'>orderId</label>
			<input type='text' id='moGetOrder-orderId-param' name='orderId'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const orderId = container.querySelector('#moGetOrder-orderId-param');

	container.querySelector('#moGetOrder-form button').onclick = () => {
		const params = {
			orderId : orderId.value !== "" ? orderId.value : undefined
		};

		moGetOrder(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { moGetOrder, moGetOrderForm };