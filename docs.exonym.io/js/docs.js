
const markdownRenderer = new marked.Renderer();
		
// markdownRenderer.table = function (header, body) {

// };

markdownRenderer.code = function(code, language) {
  const formattedCode = `<pre style="background-color: #f6f8fa; color: #24292e; border: 1px solid #d1d5da; border-radius: 3px; padding: 16px;"><code>${code}</code></pre>`;
  return formattedCode;

};

markdownRenderer.blockquote = function (quote) {
  return '<blockquote style="margin: 0 0 1em 0; padding: 0.5em 1em; border-left: 0.25em solid #dfe2e5; color: #6a737d;">' + quote + '</blockquote>';
};



function navigate(page){    
    fetch("docs/" + page)
    .then(response => response.text())
    .then(content => {
        const options = {
            gfm: true,
            breaks: false,
            headerIds: false,
            smartLists: true,
            smartypants: true,
            renderer: markdownRenderer,
            math: true,

        };
        history.pushState({ page }, null, page);        
        const contentDiv = document.querySelector('#content');
        contentDiv.innerHTML = marked(content, options);
        document.documentElement.scrollTop = 0; 
        renderMaths();

    });
}

function renderMaths(){
    MathJax.Hub.Config({
        extensions: ["tex2jax.js"],
        jax: ["input/TeX", "output/HTML-CSS"],
        tex2jax: {
            inlineMath: [['$', '$']],
            displayMath: [['$$', '$$']],
            processEscapes: true
        },
        "HTML-CSS": {
            availableFonts: ["TeX"],
            linebreaks: {
                automatic: true
            },
            showMathMenu: false
        }
    });
    MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
}

$(document).ready(()=>{
    navigate("main.md");

});

$(document).on('click', 'a', function(e) {
    if (!(e.target.href.startsWith("https://exonym.io")
	   || e.target.href.startsWith("https://github.com"))){
        e.preventDefault();
        const target = this.getAttribute('href');
        navigate(target);
        $("#navbarNav").collapse('hide');

    }
});
