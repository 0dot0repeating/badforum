var usernameCallbackID = 0;

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

        if (username !== "")
        {
            usernameCallbackID = window.setTimeout(function() { checkUsername(username, notifyElement) }, 333);
        }
    }
}


function checkUsername(username, notifyElement)
{
    var request = new XMLHttpRequest();
    request.onreadystatechange = function() {
        if (this.readyState != 4 || this.status != 200) { return; }

        var jsonResponse = JSON.parse(this.responseText);
        notifyElement.innerHTML = jsonResponse.unavailable ? "Already taken" : "Available";
    };

    request.open("GET", "/register/check/username?username=" + username);
    request.send();
}