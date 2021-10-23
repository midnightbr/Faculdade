function Enviar() {
  var nome = document.getElementById("nameid");

  if (nome.value != "") {
    alert('Obrigado Sr(a) ' + nome.value + ', os seus dados foram encaminhados com sucesso!');
  }
}