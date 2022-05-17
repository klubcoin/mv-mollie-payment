const moCreateOrder = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/moCreateOrder/`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			
		})
	});
}

const moCreateOrderForm = (container) => {
	const html = `<form id='moCreateOrder-form'>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)


	container.querySelector('#moCreateOrder-form button').onclick = () => {
		const params = {

		};

		moCreateOrder(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { moCreateOrder, moCreateOrderForm };