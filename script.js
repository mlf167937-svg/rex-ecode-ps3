// TAB SYSTEM
function openTab(id) {
  var tabs = document.getElementsByClassName("tab");
  for (var i = 0; i < tabs.length; i++) {
    tabs[i].style.display = "none";
  }
  document.getElementById(id).style.display = "block";
}

// RUN CODE
function runCode() {
  var code = document.getElementById("code").value;
  var iframe = document.getElementById("preview");

  iframe.contentWindow.document.open();
  iframe.contentWindow.document.write(code);
  iframe.contentWindow.document.close();
}

// SAVE FILE
function saveFile() {
  var name = prompt("File name:");
  var code = document.getElementById("code").value;

  if (!name) return;

  localStorage.setItem(name, code);
  alert("Saved!");
  loadFiles();
}

// LOAD FILES
function loadFiles() {
  var list = document.getElementById("fileList");
  list.innerHTML = "";

  if (localStorage.length === 0) {
    list.innerHTML = "<li>No files</li>";
    return;
  }

  for (var i = 0; i < localStorage.length; i++) {
    var key = localStorage.key(i);

    var li = document.createElement("li");

    li.innerHTML =
      key +
      " <button onclick=\"openFile('" + key + "')\">Open</button>" +
      " <button onclick=\"deleteFile('" + key + "')\">Del</button>";

    list.appendChild(li);
  }
}

// OPEN FILE
function openFile(name) {
  document.getElementById("code").value = localStorage.getItem(name);
  openTab("editor");
}

// DELETE FILE
function deleteFile(name) {
  localStorage.removeItem(name);
  loadFiles();
}
