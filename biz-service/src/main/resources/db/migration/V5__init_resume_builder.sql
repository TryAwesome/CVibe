-- V5__init_resume_builder.sql
-- Resume Builder Tables (Phase 5)

-- Resume Templates table: Stores LaTeX templates (system and user-uploaded)
CREATE TABLE IF NOT EXISTS resume_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    template_type VARCHAR(20) NOT NULL,
    category VARCHAR(30),
    latex_content TEXT NOT NULL,
    thumbnail_url VARCHAR(500),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    is_active BOOLEAN DEFAULT true,
    is_featured BOOLEAN DEFAULT false,
    usage_count INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for resume_templates
CREATE INDEX IF NOT EXISTS idx_template_type ON resume_templates(template_type);
CREATE INDEX IF NOT EXISTS idx_template_category ON resume_templates(category);
CREATE INDEX IF NOT EXISTS idx_template_user ON resume_templates(user_id);
CREATE INDEX IF NOT EXISTS idx_template_featured ON resume_templates(is_featured);

-- Resume Generations table: Records each resume generation session
CREATE TABLE IF NOT EXISTS resume_generations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    template_id UUID NOT NULL REFERENCES resume_templates(id),
    target_job_title VARCHAR(200),
    target_company VARCHAR(200),
    job_description TEXT,
    jd_file_path VARCHAR(500),
    generated_latex TEXT,
    final_latex TEXT,
    pdf_file_path VARCHAR(500),
    tailoring_notes TEXT,
    matched_keywords TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    is_exported BOOLEAN DEFAULT false,
    exported_at TIMESTAMP WITH TIME ZONE,
    user_rating INTEGER CHECK (user_rating >= 1 AND user_rating <= 5),
    user_feedback TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for resume_generations
CREATE INDEX IF NOT EXISTS idx_generation_user ON resume_generations(user_id);
CREATE INDEX IF NOT EXISTS idx_generation_status ON resume_generations(status);
CREATE INDEX IF NOT EXISTS idx_generation_created ON resume_generations(created_at);
CREATE INDEX IF NOT EXISTS idx_generation_template ON resume_generations(template_id);

-- ============================================
-- Insert sample system templates
-- ============================================

-- Modern Tech Template
INSERT INTO resume_templates (id, name, description, template_type, category, is_featured, is_active, usage_count, latex_content) VALUES
(gen_random_uuid(), 'Modern Tech', 'Clean and modern template for software engineers', 'SYSTEM', 'TECH', true, true, 150,
'\\documentclass[11pt,a4paper]{article}
\\usepackage[margin=0.75in]{geometry}
\\usepackage{enumitem}
\\usepackage{hyperref}
\\usepackage{titlesec}

\\titleformat{\\section}{\\large\\bfseries}{}{0em}{}[\\titlerule]
\\titlespacing*{\\section}{0pt}{1em}{0.5em}

\\begin{document}

% Header
\\begin{center}
    {\\LARGE\\bfseries {{NAME}}}\\\\[0.3em]
    {{EMAIL}} $\\cdot$ {{PHONE}} $\\cdot$ {{LOCATION}}\\\\
    \\href{{{LINKEDIN}}}{LinkedIn} $\\cdot$ \\href{{{GITHUB}}}{GitHub}
\\end{center}

% Summary
\\section{Summary}
{{SUMMARY}}

% Skills
\\section{Technical Skills}
{{SKILLS}}

% Experience
\\section{Professional Experience}
{{EXPERIENCE}}

% Education
\\section{Education}
{{EDUCATION}}

% Projects
\\section{Projects}
{{PROJECTS}}

% Certifications
\\section{Certifications}
{{CERTIFICATIONS}}

\\end{document}');

-- Minimal Academic Template
INSERT INTO resume_templates (id, name, description, template_type, category, is_featured, is_active, usage_count, latex_content) VALUES
(gen_random_uuid(), 'Academic CV', 'Traditional academic curriculum vitae template', 'SYSTEM', 'ACADEMIC', true, true, 85,
'\\documentclass[11pt]{article}
\\usepackage[margin=1in]{geometry}
\\usepackage{enumitem}
\\usepackage{hyperref}

\\begin{document}

\\begin{center}
    {\\Large\\textsc{{{NAME}}}}\\\\[0.5em]
    {{EMAIL}} | {{PHONE}}\\\\
    {{LOCATION}}
\\end{center}

\\section*{Research Interests}
{{SUMMARY}}

\\section*{Education}
{{EDUCATION}}

\\section*{Research Experience}
{{EXPERIENCE}}

\\section*{Publications \\& Projects}
{{PROJECTS}}

\\section*{Skills}
{{SKILLS}}

\\section*{Awards \\& Certifications}
{{CERTIFICATIONS}}

\\end{document}');

-- Professional Executive Template
INSERT INTO resume_templates (id, name, description, template_type, category, is_featured, is_active, usage_count, latex_content) VALUES
(gen_random_uuid(), 'Executive Pro', 'Professional template for senior positions', 'SYSTEM', 'PROFESSIONAL', true, true, 120,
'\\documentclass[10pt]{article}
\\usepackage[margin=0.6in]{geometry}
\\usepackage{enumitem}
\\usepackage{hyperref}
\\usepackage{xcolor}
\\definecolor{headcolor}{RGB}{0,51,102}

\\begin{document}

\\begin{center}
    {\\color{headcolor}\\Huge\\bfseries {{NAME}}}\\\\[0.5em]
    {\\large {{HEADLINE}}}\\\\[0.3em]
    {{EMAIL}} | {{PHONE}} | {{LOCATION}}\\\\
    {{LINKEDIN}} | {{PORTFOLIO}}
\\end{center}

\\vspace{1em}

{\\color{headcolor}\\Large\\bfseries Executive Summary}\\\\
\\rule{\\textwidth}{0.5pt}
{{SUMMARY}}

\\vspace{1em}
{\\color{headcolor}\\Large\\bfseries Core Competencies}\\\\
\\rule{\\textwidth}{0.5pt}
{{SKILLS}}

\\vspace{1em}
{\\color{headcolor}\\Large\\bfseries Professional Experience}\\\\
\\rule{\\textwidth}{0.5pt}
{{EXPERIENCE}}

\\vspace{1em}
{\\color{headcolor}\\Large\\bfseries Education}\\\\
\\rule{\\textwidth}{0.5pt}
{{EDUCATION}}

\\vspace{1em}
{\\color{headcolor}\\Large\\bfseries Key Projects}\\\\
\\rule{\\textwidth}{0.5pt}
{{PROJECTS}}

\\end{document}');

-- Minimal Clean Template
INSERT INTO resume_templates (id, name, description, template_type, category, is_featured, is_active, usage_count, latex_content) VALUES
(gen_random_uuid(), 'Minimal Clean', 'Simple and clean one-page resume', 'SYSTEM', 'MINIMAL', false, true, 200,
'\\documentclass[11pt]{article}
\\usepackage[margin=0.8in]{geometry}
\\usepackage{enumitem}
\\setlist{noitemsep}

\\begin{document}

\\centerline{\\Large\\bfseries {{NAME}}}
\\centerline{{{EMAIL}} | {{PHONE}} | {{LOCATION}}}

\\bigskip
\\textbf{Skills:} {{SKILLS}}

\\bigskip
\\textbf{Experience}
{{EXPERIENCE}}

\\bigskip
\\textbf{Education}
{{EDUCATION}}

\\bigskip
\\textbf{Projects}
{{PROJECTS}}

\\end{document}');

-- Modern Two-Column Template  
INSERT INTO resume_templates (id, name, description, template_type, category, is_featured, is_active, usage_count, latex_content) VALUES
(gen_random_uuid(), 'Two Column Modern', 'Contemporary two-column layout', 'SYSTEM', 'MODERN', true, true, 95,
'\\documentclass[10pt]{article}
\\usepackage[margin=0.5in]{geometry}
\\usepackage{multicol}
\\usepackage{enumitem}
\\usepackage{hyperref}

\\begin{document}

\\begin{center}
    {\\Huge\\bfseries {{NAME}}}\\\\[0.3em]
    {{HEADLINE}}\\\\[0.2em]
    {{EMAIL}} $\\bullet$ {{PHONE}} $\\bullet$ {{LOCATION}}
\\end{center}

\\begin{multicols}{2}

\\section*{About}
{{SUMMARY}}

\\section*{Skills}
{{SKILLS}}

\\section*{Certifications}
{{CERTIFICATIONS}}

\\columnbreak

\\section*{Experience}
{{EXPERIENCE}}

\\section*{Education}
{{EDUCATION}}

\\section*{Projects}
{{PROJECTS}}

\\end{multicols}

\\end{document}');
