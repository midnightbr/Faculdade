/**
 * VERSÃO NOVA
 */
function mostraRacas() {
	$.get("RacaController", {},
		function (data) {
			var racas = data;
			var objRacas = JSON.parse(racas);
			$("#divListagemRacas").html("");
			for (i = 0; i < objRacas.length; i++) {
				$("#divListagemRacas").append(objRacas[i].id + " - " + objRacas[i].nome + "<br>");
			};
		});
};

$(document).ready(function () {
	mostraRacas();

	$("#btn_alt_raca").click(function () {
		var raca = {
			id: $("#txt_id_raca").val(),
			nome: $("#txt_nome_raca").val()
		}
		$.ajax({
			url: 'RacaController',
			type: 'put',
			datatype: 'json',
			contentType: 'application/json',
			data: JSON.stringify(raca),
			success: function (data) {
				var response = data;
				var objRaca = JSON.parse(response);
				location.reload();
				alert(objRaca[1].mensagem);
			},
			complete: function (data) {
				mostraRacas();
			},
			error: function () {
				alert("Erro ao alterar raça");
			}
		});
	});
});

/**
 * VERSÃO ANTIGA
$(document).ready(function () {

	 $.get("CaninoController",
	 {
		 acao: "GET_RACAS"
		},
		function(data) {
			var racas = data;
			var objRacas = JSON.parse(racas);
			for(i = 0; i < objRacas.length; i++) {
				$("#divListagemRacas").append(objRacas[i].id + " - " + objRacas[i].nome + "<br>");
			};
		});

		$("#btn_alt_raca").click(function() {
			txtId = $("#txt_id_raca").val();
			txtNome = $("#txt_nome_raca").val();
			$.post("CaninoController",
			{
				acao: "ALTER_RACA",
				id_raca: txtId,
				rac_nome: txtNome
			},
			function(data) {
				var racas = data;
				var objRacas = JSON.parse(racas);
				$("#divListagemCaes").html("");
				for(i = 0; i < objRacas.length; i++) {
					$("#divListagemRacas").append(objRacas[i].id + " - " + objRacas[i].nome + "<br>");
			};
		});
	});
});
*/