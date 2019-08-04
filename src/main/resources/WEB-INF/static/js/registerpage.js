var usernameCallbackID = 0;
var passwordCallbackID = 0;

function onUsernameInput(inputElement, notifyID)
{
    if (usernameCallbackID !== 0)
    {
        window.clearTimeout(usernameCallbackID);
    }

    var username = inputElement.value;
    var notifyElement = document.getElementById(notifyID);

    if (notifyElement)
    {
        notifyElement.innerHTML = "";
        usernameCallbackID = window.setTimeout(function() { checkUsername(username, notifyElement) }, 333);
    }
}


function onPasswordInput()
{
    if (passwordCallbackID !== 0)
    {
        window.clearTimeout(passwordCallbackID);
    }

    var notifyElement = document.getElementById("passwordnote");

    if (notifyElement)
    {
        notifyElement.innerHTML = "";
        passwordCallbackID = window.setTimeout(checkPassword, 333);
    }
}


function checkPassword()
{
    var passwordElement = document.getElementById("passwordInput");
    var confirmElement  = document.getElementById("confirmInput");
    var notifyElement   = document.getElementById("passwordnote");

    if (passwordElement.value === "")
    {
        notifyElement.innerHTML = "Password is mandatory";
        return;
    }

    if (confirmElement.value === "")
    {
        notifyElement.innerHTML = "Confirm your password";
        return;
    }

    if (passwordElement.value !== confirmElement.value)
    {
        notifyElement.innerHTML = "Passwords don't match";
    }
}


function checkUsername(username, notifyElement)
{
    if (username === "")
    {
        notifyElement.innerHTML = "Username is mandatory";
        return;
    }

    var request = new XMLHttpRequest();
    request.onreadystatechange = function() {
        if (this.readyState != 4 || this.status != 200) { return; }

        var jsonResponse = JSON.parse(this.responseText);
        notifyElement.innerHTML = jsonResponse.available ? "Available" : "Already taken";
    };

    request.open("GET", "/api/checkUsername?username=" + username);
    request.send();
}


function jsonRegister()
{
    console.log("benis");
}