$(document).ready(function () {
	$.get("CaninoController",
          {
               acao: "GET_CAES"
          },
          function (data) {
               var caes = data;
               var objCaes = JSON.parse(caes);
               for (i = 0; i < objCaes.length; i++) {
                    $("#divListagemCaes").append(objCaes[i].id + " - " + objCaes[i].nome + " (" + objCaes[i].raca.nome + ")" + "<br>");
               };
        });
	$("#btn_del_cao").click(function () {
          txtId = $("#txt_id_cao").val();
          $.post("CaninoController",
               {
                    acao: "DELETE_CAO",
                    id_cao: txtId
               },
               function (data) {
                    var cao = data;
                    var objCao = JSON.parse(cao);
                    $("#divMensagem").append(objCao[1].mensagem);

                    $.get("CaninoController",
                         {
                              acao: "GET_CAES"
                         },
                         function (data) {
                              var caes = data;
                              var objCaes = JSON.parse(caes);
                              $("#divListagemCaes").html("");
                              for (i = 0; i < objCaes.length; i++) {
                                   $("divListagemCaes").append(objCaes[i] + " - " + objCaes[i].nome + " (" + objCaes[i].raca.nome + ")" + "<br>");
                              };
                         });
               });
     });
});