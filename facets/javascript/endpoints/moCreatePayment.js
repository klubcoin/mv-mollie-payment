const moCreatePayment = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/moCreatePayment/`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			
		})
	});
}

const moCreatePaymentForm = (container) => {
	const html = `<form id='moCreatePayment-form'>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)


	container.querySelector('#moCreatePayment-form button').onclick = () => {
		const params = {

		};

		moCreatePayment(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { moCreatePayment, moCreatePaymentForm };