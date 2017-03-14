$(document).ready(function(){
    showHideDetails();
});

function showHideDetails() {
    var emailDetailsYes = $('#hidden-email-details-yes');
    var emailDetailsNo = $('#hidden-email-details-no');

    emailDetailsYes.hide();
    emailDetailsNo.hide();

    $('input[type=radio][name=useEmailAddress]').change(function(){
        if(this.value == 'true') {
            emailDetailsYes.show();
            emailDetailsNo.hide();
        } else {
            emailDetailsYes.hide();
            emailDetailsNo.show();
        }
    });

    if($('#useEmailAddress-true').attr('checked')) {
        emailDetailsYes.show();
        emailDetailsNo.hide();
    } else if($('#useEmailAddress-false').attr('checked')) {
        emailDetailsYes.hide();
        emailDetailsNo.show();
    }
}
