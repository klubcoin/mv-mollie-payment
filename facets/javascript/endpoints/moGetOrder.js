const moGetOrder = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/moGetOrder/${parameters.orderId}`, baseUrl);
	if (parameters.embed !== undefined) {
		url.searchParams.append('embed', parameters.embed);
	}

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
		<div id='moGetOrder-embed-form-field'>
			<label for='embed'>embed</label>
			<input type='text' id='moGetOrder-embed-param' name='embed'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const orderId = container.querySelector('#moGetOrder-orderId-param');
	const embed = container.querySelector('#moGetOrder-embed-param');

	container.querySelector('#moGetOrder-form button').onclick = () => {
		const params = {
			orderId : orderId.value !== "" ? orderId.value : undefined,
			embed : embed.value !== "" ? embed.value : undefined
		};

		moGetOrder(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { moGetOrder, moGetOrderForm };