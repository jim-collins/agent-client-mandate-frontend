$(document).ready(function(){
var agree = document.getElementById("agree");// checkbox
var continueBtn = document.getElementById("submit");//submit and continue button
continueBtn.disabled = true;

agree.onclick = function() {

    if (agree.checked == true)
    {
        continueBtn.disabled = false;
    }
    else
    {
        continueBtn.disabled = true;
    }
}
});
