var cordova = require('cordova');

alert("Entro aqui");

function GPSDanBreakingNews() {
}

GPSDanBreakingNews.prototype.getGPSLocation = function (arg0, success, error) {
  cordova.exec(success, error, "GPSDanBreakingNews", "getGPSLocation", [arg0]);
};


GPSDanBreakingNews.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }

  window.plugins.GPSDanBreakingNews = new GPSDanBreakingNews();
  return window.plugins.GPSDanBreakingNews;
};

alert("antes de el constructor");
cordova.addConstructor(GPSDanBreakingNews.install);
alert("Completo el trabajo");