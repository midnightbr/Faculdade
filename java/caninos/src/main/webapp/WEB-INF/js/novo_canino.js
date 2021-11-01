$(document).ready(function () {
     $.get("CaninoController",
          {
               acao: "GET_CAES"
          },
          function (data) {
               var caes = data;
               var objCaes = JSON.parse(caes);
               for (i = 0; i < objCaes.length; i++) {
                    $("divListagemCaes").append(objCaes[i].id + " - " + objCaes[i].nome + " (" + objCaes[i].raca.nome + ")" + "<br>");
               };
          });
     $.get("CaninoController",
          {
               acao: "GET_RACAS"
          },
          function (data) {
               var racas = data;
               var objRacas = JSON.parse(racas);
               for (i = 0; i < objRacas.length; i++) {
                    $("#sel_raca").append("<option value=" + objRacas[i] + ">" + objRacas[i].nome + "</option>")
               }
          });
     $("#btn_add_cao").click(function () {
          txtId = $("#txt_id_cao").val();
          txtNome = $("#txt_nome_cao").val();
          selSexo = $("#sel_sexo_cao").val();
          selRaca = $("#sel_raca").val();
          $.post("CaninoController",
               {
                    acao: "INSERT_CAO",
                    id: txtId,
                    nome: txtNome,
                    sexo: selSexo,
                    raca: selRaca
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