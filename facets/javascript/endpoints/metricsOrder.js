const metricsOrder = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/metricsOrder/`, baseUrl);
	return fetch(url.toString(), {
		method: 'GET'
	});
}

const metricsOrderForm = (container) => {
	const html = `<form id='metricsOrder-form'>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)


	container.querySelector('#metricsOrder-form button').onclick = () => {
		const params = {

		};

		metricsOrder(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { metricsOrder, metricsOrderForm };