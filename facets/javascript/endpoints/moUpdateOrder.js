const moUpdateOrder = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/moUpdateOrder/`, baseUrl);
	return fetch(url.toString(), {
		method: 'PUT'
	});
}

const moUpdateOrderForm = (container) => {
	const html = `<form id='moUpdateOrder-form'>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)


	container.querySelector('#moUpdateOrder-form button').onclick = () => {
		const params = {

		};

		moUpdateOrder(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { moUpdateOrder, moUpdateOrderForm };