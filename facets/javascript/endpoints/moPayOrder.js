const moPayOrder = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/moPayOrder/${parameters.orderId}`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			data : parameters.data
		})
	});
}

const moPayOrderForm = (container) => {
	const html = `<form id='moPayOrder-form'>
		<div id='moPayOrder-orderId-form-field'>
			<label for='orderId'>orderId</label>
			<input type='text' id='moPayOrder-orderId-param' name='orderId'/>
		</div>
		<div id='moPayOrder-data-form-field'>
			<label for='data'>data</label>
			<input type='text' id='moPayOrder-data-param' name='data'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const orderId = container.querySelector('#moPayOrder-orderId-param');
	const data = container.querySelector('#moPayOrder-data-param');

	container.querySelector('#moPayOrder-form button').onclick = () => {
		const params = {
			orderId : orderId.value !== "" ? orderId.value : undefined,
			data : data.value !== "" ? data.value : undefined
		};

		moPayOrder(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { moPayOrder, moPayOrderForm };