$(document).ready(function(){
    showHideDetails();
});

function showHideDetails() {
    var emailDetailsYes = $('#hidden-email-details-yes');
    var emailDetailsNo = $('#hidden-email-details-no');
    var submitButton = $('#hidden-submit-button');
    var submitLink = $('#hidden-submit-link');

    emailDetailsYes.hide();
    emailDetailsNo.hide();
    submitLink.hide();

    $('input[type=radio][name=useEmailAddress]').change(function(){
        if(this.value == 'true') {
            emailDetailsYes.show();
            submitButton.show();
            emailDetailsNo.hide();
            submitLink.hide();

        } else {
            emailDetailsYes.hide();
            submitButton.hide();
            emailDetailsNo.show();
            submitLink.show();
        }
    });

    if($('#useEmailAddress-true').attr('checked')) {
        emailDetailsYes.show();
        submitButton.show();
        emailDetailsNo.hide();
        submitLink.hide();
    } else if($('#useEmailAddress-false').attr('checked')) {
        emailDetailsYes.hide();
        submitButton.hide();
        emailDetailsNo.show();
        submitLink.show();
    }
}
