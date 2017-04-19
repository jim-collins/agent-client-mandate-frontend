function showHideIdentifiersSection() {
    var selectedDiv = $('#hidden-ocrDetails');
    var businessUniqueIdField = $('#idNumber');
    var issuingCountryField = $('#issuingCountryCode');
    var issuingCountryHiddenField = $('#issuingCountryCode_');
    var issuingInstitutionField = $('#issuingInstitution');
    selectedDiv.hide();

    if($('#hasBusinessUniqueId-true').is(':checked')) {
        selectedDiv.show();
    }

    $('input[type=radio][name=hasBusinessUniqueId]').change(function(){
        if(this.value == 'true') {
            selectedDiv.show();
        } else {
            selectedDiv.hide();
            businessUniqueIdField.val('');
            issuingCountryHiddenField.prop('selectedIndex',0);
            issuingCountryField.val('');
            issuingInstitutionField.val('');
        }
    });

}
