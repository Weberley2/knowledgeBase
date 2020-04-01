function post(path, params, method) {
    method = method || "post"; // Set method to post by default if not specified.

    // The rest of this code assumes you are not using a library.
    // It can be made less wordy if you use one.
    var form = document.createElement("form");
    form.style.visibility = "hidden"; 
    form.setAttribute("method", method);
    form.setAttribute("action", path);

    for(var key in params) {
        if(params.hasOwnProperty(key)) {
            var hiddenField = document.createElement("input");
            hiddenField.style.visibility = "hidden"; 
            hiddenField.setAttribute("type", "hidden");
            hiddenField.setAttribute("name", key);
            hiddenField.setAttribute("value", params[key]);

            form.appendChild(hiddenField);
        }
    }

    document.body.appendChild(form);
    form.submit();
}
function focusSearchBar(){
   if( /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ) {
 	return;
   }
   
}

function getNextDocumentID(){
    var request = new XMLHttpRequest();
    request.open('GET', '/nextdocumentid', false);  // `false` makes the request synchronous
    request.send(null);

    if (request.status !== 200) {
    	return -1;
    }
    return request.responseText.substring(4, request.responseText.length - 5);
}

function getCurrentDocumentID(){
    splitURL = window.location.href.split("/");
    id = splitURL[splitURL.length -1];
    return id;
}

function createDocument(){
    dict = {action : 'createDocument', title : document.getElementById('titleInput').value, content : document.getElementById('contentInput').value, tags : document.getElementById('tagsInput').value};
    id = getNextDocumentID();
    post('/' + String(id), dict);
}
function editDocument(){
    id = getCurrentDocumentID();
    dict = {action : 'editDocument', title : document.getElementById('titleInput').value, content : document.getElementById('contentInput').value, tags : document.getElementById('tagsInput').value, id : String(id)};
    post('/' + String(id), dict);
}

function deleteDocument(){
    id = getCurrentDocumentID();
    dict = {action : 'deleteDocument', id : String(id)};
    post('/', dict);
}

function showDeleteDocumentDiv(){
    document.getElementById("questionDiv").style.visibility = "visible";
    document.getElementById("questionDiv").style.zIndex = "1";
}

function hideDeleteDocumentDiv(){
    document.getElementById("questionDiv").style.visibility = "hidden";
    document.getElementById("questionDiv").style.zIndex = "-1";
}

function changeLocation(newLocation){
    dict = {action : 'none', source : window.location.href};
    post(newLocation, dict);
}

function searchQuery(){
    query = encodeURI(document.getElementById("searchBar").value);
    document.location.href = "/results?" + query;

}


function focusSearchBar(){
   if( /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ) {
 	return;
   }
   searchBar.focus();
}

function addEventListenerToSearchBar(){
    searchBar = document.getElementById("searchBar");
    searchBar.addEventListener("keyup", function(event) {
    if (event.key === "Enter") {
        searchQuery();
    }
});
}

function addEventListenerToLoginBar(){
    searchBar = document.getElementById("searchBar");
    searchBar.addEventListener("keyup", function(event) {
    if (event.code === "Enter") {
        dict = {action : 'login', password:encodeURI(document.getElementById("searchBar").value)};
    post('/', dict);
    }
});
}

function insertTab(o, e)
{		
	var kC = e.keyCode ? e.keyCode : e.charCode ? e.charCode : e.which;
	if (kC == 9 && !e.shiftKey && !e.ctrlKey && !e.altKey)
	{
		var oS = o.scrollTop;
		if (o.setSelectionRange)
		{
			var sS = o.selectionStart;	
			var sE = o.selectionEnd;
			o.value = o.value.substring(0, sS) + "\t" + o.value.substr(sE);
			o.setSelectionRange(sS + 1, sS + 1);
			o.focus();
		}
		else if (o.createTextRange)
		{
			document.selection.createRange().text = "\t";
			e.returnValue = false;
		}
		o.scrollTop = oS;
		if (e.preventDefault)
		{
			e.preventDefault();
		}
		return false;
	}
	return true;
}

function setTextAreaHeight(){
	element = document.getElementById("contentInput");
	var rect = element.getBoundingClientRect();
	var startY = rect.top;
	var endY = Math.max(document.documentElement.clientHeight, window.innerHeight || 0) - 150;
	var height = endY - startY;
	element.style.height = String(height) + "px";
}




