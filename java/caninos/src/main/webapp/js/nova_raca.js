/**
 * CONTROLA A INTERFACE  COM O USUÁRIO NOVA_RACA.HTML
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
     mostraRacas();  // Chamando a função para preencher div com as Raças

     $("#btn_add_raca").click(function () {
          var raca = {
               id: $("#txt_id_raca").val(),
               nome: $("#txt_nome_raca").val()
          }
          $.ajax({
               url: 'RacaController',
               type: 'post',
               datatype: 'json',
               contentType: 'application/json',
               data: JSON.stringify(raca),
               success: function (data) {
                    var response = data;
                    var objRaca = JSON.parse(response);
                    location.reload();
                    alert(objRaca[1].mensagem);
                    // $("#divMensagem").append(objRaca[1].mensagem);
               },
               complete: function (data) {
                    mostraRacas();
               },
               error: function () {
                    $("#divMensagem").html("Erro ao inserir");
               }
          });
     });
});
/*
PROJETO VERSÃO ANTIGA
$(document).ready(function () {

     $.get("CaninoController",
          {
               acao: "GET_RACAS"
          },
          function (data) {
               var racas = data;
               var objRacas = JSON.parse(racas);
               for (i = 0; i < objRacas.length; i++) {
                    $("#divListagemRacas").append(objRacas[i].id + " - " + objRacas[i].nome + "<br>");
               };
          });

     $("#btn_add_raca").click(function () {
          txtId = $("#txt_id_raca").val();
          txtNome = $("#txt_nome_raca").val();
          $.post("CaninoController",
               {
                    acao: "INSERT_RACA",
                    id_raca: txtId,
                    nome_raca: txtNome
               },
               function (data) {
                    var racas = data;
                    var objRacas = JSON.parse(racas);
                    $("#divListagemCaes").html("");
                    for (i = 0; i < objRacas.length; i++) {
                         $("divListagemRacas").append(objRacas[i].id + " - " + objRacas[i].nome + "<br>");
                    };
               });
     });
});
*/