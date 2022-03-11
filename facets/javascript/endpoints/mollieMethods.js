const mollieMethods = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/mollieMethods/`, baseUrl);
	if (parameters.method !== undefined) {
		url.searchParams.append('method', parameters.method);
	}

	return fetch(url.toString(), {
		method: 'GET'
	});
}

const mollieMethodsForm = (container) => {
	const html = `<form id='mollieMethods-form'>
		<div id='mollieMethods-null-form-field'>
			<label for='null'>null</label>
			<input type='text' id='mollieMethods-null-param' name='null'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const null = container.querySelector('#mollieMethods-null-param');

	container.querySelector('#mollieMethods-form button').onclick = () => {
		const params = {
			null : null.value !== "" ? null.value : undefined
		};

		mollieMethods(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { mollieMethods, mollieMethodsForm };