require 'asciidoctor-pdf' unless defined? ::Asciidoctor::Pdf

module AsciidoctorPdfExtensions

#	def layout_title_page doc
#		if (doc.attr? 'testvar')
#			puts (doc.attr 'testvar')
#			title_align = (@theme.title_page_align || @base_align).to_sym
#			
#			theme_font :title_page do
#			indent (@theme.title_page_revision_margin_left || 0), (@theme.title_page_revision_margin_right || 0) do
#				theme_font :title_page do
#					layout_prose (doc.attr 'testvar'),
#              align: title_align,
#              margin: 0,
#              normalize: false
#        end
#      end
#        
#		else
#			puts "NOTOC"
#		end
#		super
#	end
	
	def layout_title_page doc
    return unless doc.header? && !doc.notitle

    prev_bg_image = @page_bg_image
    prev_bg_color = @page_bg_color

    if (bg_image = resolve_background_image doc, @theme, 'title-page-background-image')
      @page_bg_image = (bg_image == 'none' ? nil : bg_image)
    end
    if (bg_color = resolve_theme_color :title_page_background_color)
      @page_bg_color = bg_color
    end
    # NOTE a new page will already be started if the cover image is a PDF
    start_new_page unless page_is_empty?
    start_new_page if @ppbook && verso_page?
    @page_bg_image = prev_bg_image if bg_image
    @page_bg_color = prev_bg_color if bg_color

    # IMPORTANT this is the first page created, so we need to set the base font
    font @theme.base_font_family, size: @theme.base_font_size

    # QUESTION allow alignment per element on title page?
    title_align = (@theme.title_page_align || @base_align).to_sym

    # TODO disallow .pdf as image type
    if (logo_image_path = (doc.attr 'title-logo-image', @theme.title_page_logo_image))
      if (logo_image_path.include? ':') && logo_image_path =~ ImageAttributeValueRx
        logo_image_path = $1
        logo_image_attrs = (AttributeList.new $2).parse ['alt', 'width', 'height']
        relative_to_imagesdir = true
      else
        logo_image_attrs = {}
        relative_to_imagesdir = false
      end
      # HACK quick fix to resolve image path relative to theme
      logo_image_path = ThemeLoader.resolve_theme_asset logo_image_path, (doc.attr 'pdf-stylesdir') unless doc.attr? 'title-logo-image'
      logo_image_attrs['target'] = logo_image_path
      logo_image_attrs['align'] ||= (@theme.title_page_logo_align || title_align.to_s)
      # QUESTION should we allow theme to turn logo image off?
      logo_image_top = logo_image_attrs['top'] || @theme.title_page_logo_top || '10%'
      # FIXME delegate to method to convert page % to y value
      if logo_image_top.end_with? 'vh'
        logo_image_top = page_height - page_height * logo_image_top.to_f / 100.0
      else
        logo_image_top = bounds.absolute_top - effective_page_height * logo_image_top.to_f / 100.0
      end
      initial_y, @y = @y, logo_image_top
      # FIXME add API to Asciidoctor for creating blocks like this (extract from extensions module?)
      image_block = ::Asciidoctor::Block.new doc, :image, content_model: :empty, attributes: logo_image_attrs
      # NOTE pinned option keeps image on same page
      indent (@theme.title_page_logo_margin_left || 0), (@theme.title_page_logo_margin_right || 0) do
        convert_image image_block, relative_to_imagesdir: relative_to_imagesdir, pinned: true
      end
      @y = initial_y
    end

    # TODO prevent content from spilling to next page
    theme_font :title_page do                  
      
      #university
      move_down(@theme.title_page_mater_margin_top || 0)
      indent (@theme.title_page_mater_margin_left || 0), (@theme.title_page_mater_margin_right || 0) do
        theme_font :title_page_mater do
          layout_heading (doc.attr 'university'),
            align: title_align,
            margin: 0,
            line_height: @theme.title_page_mater_line_height
        end
      end
      move_down(@theme.title_page_title_margin_bottom || 0)
      
      #faculty
      move_down(@theme.title_page_mater_margin_top || 0)
      indent (@theme.title_page_mater_margin_left || 0), (@theme.title_page_mater_margin_right || 0) do
        theme_font :title_page_mater do
          layout_heading (doc.attr 'faculty'),
            align: title_align,
            margin: 0,
            line_height: @theme.title_page_mater_line_height
        end
      end
      move_down(@theme.title_page_title_margin_bottom || 0)
      
      #department
      move_down(@theme.title_page_mater_margin_top || 0)
      indent (@theme.title_page_mater_margin_left || 0), (@theme.title_page_mater_margin_right || 0) do
        theme_font :title_page_mater do
          layout_heading (doc.attr 'department'),
            align: title_align,
            margin: 0,
            line_height: @theme.title_page_mater_line_height
        end
      end
      move_down(@theme.title_page_title_margin_bottom || 0)
      
      doctitle = doc.doctitle partition: true
      if (title_top = @theme.title_page_title_top)
        if title_top.end_with? 'vh'
          title_top = page_height - page_height * title_top.to_f / 100.0
        else
          title_top = bounds.absolute_top - effective_page_height * title_top.to_f / 100.0
        end
        # FIXME delegate to method to convert page % to y value
        @y = title_top
      end
      move_down(@theme.title_page_title_margin_top || 0)
      indent (@theme.title_page_title_margin_left || 0), (@theme.title_page_title_margin_right || 0) do
        theme_font :title_page_title do
          layout_heading doctitle.main,
            align: title_align,
            margin: 0,
            line_height: @theme.title_page_title_line_height
        end
      end
      move_down(@theme.title_page_title_margin_bottom || 0)
      
      if doctitle.subtitle
        move_down(@theme.title_page_subtitle_margin_top || 0)
        indent (@theme.title_page_subtitle_margin_left || 0), (@theme.title_page_subtitle_margin_right || 0) do
          theme_font :title_page_subtitle do
            layout_heading doctitle.subtitle,
              align: title_align,
              margin: 0,
              line_height: @theme.title_page_subtitle_line_height
          end
        end
        move_down(@theme.title_page_subtitle_margin_bottom || 0)
      end
      
      author_align = (@theme.title_page_authors_align || title_align).to_sym
      if doc.attr? 'authors'        
        move_down(@theme.title_page_authors_margin_top || 0)
        indent (@theme.title_page_authors_margin_left || 0), (@theme.title_page_authors_margin_right || 0) do
          # TODO provide an API in core to get authors as an array
          authors = (1..(doc.attr 'authorcount', 1).to_i).map {|idx|
            doc.attr(idx == 1 ? 'author' : %(author_#{idx}))
          } * (@theme.title_page_authors_delimiter || ', ')
          theme_font :title_page_authors do
            layout_prose authors,
              align: author_align,
              margin: 0,
              normalize: false
          end
        end
        move_down(@theme.title_page_authors_margin_bottom || 0)
      end
      
      #field of study
      move_down(@theme.title_page_authors_margin_top || 0)
      indent (@theme.title_page_authors_margin_left || 0), (@theme.title_page_authors_margin_right || 0) do
        theme_font :title_page_authors do
          layout_prose (doc.attr 'field_of_study'),
            align: author_align,
              margin: 0,
              normalize: false
        end
      end
      move_down(@theme.title_page_authors_margin_bottom || 0)
      
      revision_info = [(doc.attr? 'revnumber') ? %(#{doc.attr 'version-label'} #{doc.attr 'revnumber'}) : nil, (doc.attr 'revdate')].compact
      unless revision_info.empty?
        move_down(@theme.title_page_revision_margin_top || 0)
        revision_text = revision_info * (@theme.title_page_revision_delimiter || ', ')
        indent (@theme.title_page_revision_margin_left || 0), (@theme.title_page_revision_margin_right || 0) do
          theme_font :title_page_revision do
            layout_prose revision_text,
              align: title_align,
              margin: 0,
              normalize: false
          end
        end
        move_down(@theme.title_page_revision_margin_bottom || 0)
      end
    end
  end
	
  def layout_toc doc, num_levels = 2, toc_page_number = 2, num_front_matter_pages = 0, start_at = nil
    go_to_page toc_page_number unless (page_number == toc_page_number) || scratch?


    offset = 3
    toc_page_numbers = super doc, num_levels, (toc_page_number + offset), num_front_matter_pages
    scratch? ? ((toc_page_numbers.begin - offset)..toc_page_numbers.end) : toc_page_numbers
    
  end

 
	def layout_chapter_title node, title, opts
    puts "title" + title
    # delegate to default implementation
    typeset_text "blabla " + "sdfsdf", calc_line_metrics((opts.delete :line_height) || @theme[%(heading_h#{opts[:level]}_line_height)] || @theme.heading_line_height), {
      color: @font_color,
      inline_format: true,
      align: @base_align.to_sym
    }.merge(opts)
    super
  end
  
end

Asciidoctor::Pdf::Converter.prepend AsciidoctorPdfExtensions
