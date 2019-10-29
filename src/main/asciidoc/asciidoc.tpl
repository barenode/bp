{% extends 'display_priority.tpl' %}


{% block input %}
{% if resources.global_content_filter.include_input_prompt %}
{%- endif -%}
[source
{%- if 'magics_language' in cell.metadata  -%}
   , {{ cell.metadata.magics_language}}
{%- elif 'pygments_lexer' in nb.metadata.get('language_info', {}) -%}
   , {{ nb.metadata.language_info.pygments_lexer }}
{%- elif 'name' in nb.metadata.get('language_info', {}) -%}
   , {{ nb.metadata.language_info.name }}
{%- endif -%}]
----
{{ cell.source}}
----
{% endblock input %}

{% block output_group %}
{% if resources.global_content_filter.include_output_prompt %}
{%- endif %}
{{- super() -}}
{% endblock output_group %}

{% block error %}
{{ super() }}
{% endblock error %}

{% block traceback_line %}
{{ line | indent | strip_ansi }}
{% endblock traceback_line %}

{%- block execute_result %}
{%- block data_priority scoped %}
{{- super() -}}
{%- endblock %}
{%- endblock execute_result %}

{% block stream %}
----
{{ output.text -}}
----
{% endblock stream %}

{% block data_svg %}
image::../{{ output.metadata.filenames['image/svg+xml'] | path2url }}[]
{% endblock data_svg %}

{% block data_png %}

[[{{cell.metadata.ref}}]]
image::../{{ output.metadata.filenames['image/png'] | path2url }}[title="{{cell.metadata.title}}"]
{% endblock data_png %}

{% block data_jpg %}
image::../{{ output.metadata.filenames['image/jpeg'] | path2url }}[]
{% endblock data_jpg %}

{% block data_latex %}
{{ output.data['text/latex'] | convert_pandoc(from_format="latex", to_format="asciidoc")}}
{% endblock data_latex %}

{% block data_html scoped %}
{{ output.data['text/html'] | convert_pandoc(from_format='html', to_format='asciidoc')}}
{% endblock data_html %}

{% block data_markdown scoped %}
{{ output.data['text/markdown'] | markdown2asciidoc}}
{% endblock data_markdown %}

{% block data_text scoped %}
{% endblock data_text %}

{% block markdowncell scoped %}
{{ cell.source | markdown2asciidoc}}
{% endblock markdowncell %}

{% block unknowncell scoped %}
unknown type  {{ cell.type }}
{% endblock unknowncell %}
