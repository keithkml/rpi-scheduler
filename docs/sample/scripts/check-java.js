var javawsInstalled = 0;
var javaws142Installed=0;
var javaws150Installed=0;
var unknownjavaws = true;
isIE = "false";
if (navigator.mimeTypes && navigator.mimeTypes.length) {
   if (navigator.mimeTypes['application/x-java-jnlp-file']) {
      javawsInstalled = 1;
   }
} else {
   isIE = "true";
}