// common file for all form widget javascript functions

// prevents doubleposts for <submit> widgets of type "button" or "image"
function submitFormWithSingleClick(button) {
    // only disable when the form action is defined, otherwise forms that 
    // return to the same page by not specifying an action will continue to
    // heve the button disabled in IE and maybe other browsers
    if (button.form.action != null && button.form.action.length > 0) {
        button.disabled = true;
    }
    button.form.submit();
}

