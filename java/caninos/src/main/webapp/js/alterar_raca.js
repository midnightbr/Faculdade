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