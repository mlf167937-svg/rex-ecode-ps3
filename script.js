// NAVIGATION SYSTEM
function openApp(app) {
  var apps = document.getElementsByClassName("app");
  for (var i = 0; i < apps.length; i++) {
    apps[i].style.display = "none";
  }
  document.getElementById(app).style.display = "block";
}

// RUN CODE
function runCode() {
  var code = document.getElementById("code").value;
  var iframe = document.getElementById("preview");

  iframe.contentWindow.document.open();
  iframe.contentWindow.document.write(code);
  iframe.contentWindow.document.close();
}

// SAVE FILE (localStorage)
function saveFile() {
  var name = prompt("File name:");
  var code = document.getElementById("code").value;

  if (name) {
    localStorage.setItem(name, code);
    alert("Saved!");
    loadFiles();
  }
}

// LOAD FILE LIST
function loadFiles() {
  var list = document.getElementById("fileList");
  list.innerHTML = "";

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
  var data = localStorage.getItem(name);
  document.getElementById("code").value = data;
  openApp("editor");
}

// DELETE FILE
function deleteFile(name) {
  localStorage.removeItem(name);
  loadFiles();
}