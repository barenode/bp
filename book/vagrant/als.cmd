vagrant ssh -c "/usr/local/rvm/gems/default/gems/asciidoctor-pdf-1.5.0.beta.2/bin/asciidoctor-pdf -a pdf-style=/opt/dev/themes/basic-theme.yml -a pdf-fontsdir=/opt/dev/fonts -r asciidoctor-mathematical /opt/dev/recommendation-implicit-als.adoc"