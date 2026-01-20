"""
Resume Parser - 从 PDF/图像简历提取结构化数据
================================================================================

支持:
- PDF 文件解析
- 图像 OCR 解析
- 使用 LLM/VLM 进行结构化提取

提取内容:
- 个人信息 (姓名, 邮箱, 电话, LinkedIn, GitHub, 个人网站)
- 个人简介
- 工作经历 (公司, 职位, 时间, 职责, 成就, 技术栈)
- 教育经历 (学校, 学位, 专业, GPA, 时间, 活动)
- 项目经历 (名称, 描述, 技术栈, 链接, 亮点)
- 技能列表 (技能名称, 熟练程度)
- 证书/荣誉
- 语言能力
"""

import io
import json
import re
from typing import Optional, Union
from dataclasses import dataclass, field

from PyPDF2 import PdfReader

from ...llm import LLMClient, VisionClient


@dataclass
class ParsedResume:
    """解析后的简历数据 - 完整结构"""
    raw_text: str = ""
    
    # 个人信息
    name: str = ""
    email: str = ""
    phone: str = ""
    linkedin: str = ""
    github: str = ""
    website: str = ""
    location: str = ""
    
    # 概要
    summary: str = ""
    headline: str = ""  # 职业头衔/目标职位
    
    # 工作经历
    work_experience: list[dict] = field(default_factory=list)
    # 每项: {company, title, location, employment_type, start_date, end_date, 
    #        is_current, description, achievements[], technologies[]}
    
    # 教育经历
    education: list[dict] = field(default_factory=list)
    # 每项: {school, degree, field, location, start_date, end_date, 
    #        gpa, description, activities[], honors[]}
    
    # 项目经历
    projects: list[dict] = field(default_factory=list)
    # 每项: {name, description, url, repo_url, technologies[], 
    #        start_date, end_date, highlights[]}
    
    # 技能
    skills: list[dict] = field(default_factory=list)
    # 每项: {name, level, category}  level: BEGINNER/INTERMEDIATE/ADVANCED/EXPERT
    
    # 证书
    certifications: list[dict] = field(default_factory=list)
    # 每项: {name, issuer, date, url}
    
    # 荣誉/奖项
    achievements: list[str] = field(default_factory=list)
    
    # 语言能力
    languages: list[dict] = field(default_factory=list)
    # 每项: {language, proficiency}


# 详细的简历解析提示词
RESUME_PARSE_PROMPT = """你是一位专业的简历解析专家。请仔细分析以下简历内容，提取所有结构化信息。

**重要要求**:
1. 尽可能提取所有可用信息，不要遗漏
2. 日期格式统一为 YYYY-MM (如: 2023-06)
3. 如果是"至今"或"present"，end_date 填写 "present"
4. 技能熟练度根据上下文判断: BEGINNER/INTERMEDIATE/ADVANCED/EXPERT
5. 对于工作经历中的成就，尽量提取具体数字和量化指标
6. 如果简历是英文，保持原文；如果是中文，保持原文

**输出格式** (严格 JSON，不要有其他内容):
```json
{
    "name": "姓名",
    "email": "邮箱地址",
    "phone": "电话号码",
    "linkedin": "LinkedIn URL (如果有)",
    "github": "GitHub URL (如果有)",
    "website": "个人网站 (如果有)",
    "location": "所在地",
    "headline": "职业头衔或目标职位",
    "summary": "个人简介/自我评价",
    
    "work_experience": [
        {
            "company": "公司名称",
            "title": "职位名称",
            "location": "工作地点",
            "employment_type": "FULL_TIME/PART_TIME/CONTRACT/INTERNSHIP/FREELANCE",
            "start_date": "YYYY-MM",
            "end_date": "YYYY-MM 或 present",
            "is_current": true/false,
            "description": "工作描述",
            "achievements": ["成就1 (尽量量化)", "成就2"],
            "technologies": ["使用的技术1", "技术2"]
        }
    ],
    
    "education": [
        {
            "school": "学校名称",
            "degree": "学位 (本科/硕士/博士/Bachelor/Master/PhD)",
            "field": "专业/领域",
            "location": "学校地点",
            "start_date": "YYYY-MM",
            "end_date": "YYYY-MM 或 present",
            "gpa": "GPA (如果有)",
            "description": "描述 (如果有)",
            "activities": ["课外活动1", "活动2"],
            "honors": ["荣誉1", "奖学金"]
        }
    ],
    
    "projects": [
        {
            "name": "项目名称",
            "description": "项目描述",
            "url": "项目链接 (如果有)",
            "repo_url": "代码仓库链接 (如果有)",
            "technologies": ["技术栈1", "技术2"],
            "start_date": "YYYY-MM (如果有)",
            "end_date": "YYYY-MM (如果有)",
            "highlights": ["亮点1", "亮点2"]
        }
    ],
    
    "skills": [
        {
            "name": "技能名称",
            "level": "BEGINNER/INTERMEDIATE/ADVANCED/EXPERT",
            "category": "编程语言/框架/工具/数据库/云服务/其他"
        }
    ],
    
    "certifications": [
        {
            "name": "证书名称",
            "issuer": "颁发机构",
            "date": "获得日期 YYYY-MM",
            "url": "验证链接 (如果有)"
        }
    ],
    
    "achievements": ["获得的荣誉/奖项1", "奖项2"],
    
    "languages": [
        {
            "language": "语言名称",
            "proficiency": "Native/Fluent/Professional/Basic"
        }
    ]
}
```

**简历内容**:
{resume_text}

请只输出 JSON，不要有任何其他说明文字。"""


VISION_PARSE_PROMPT = """You are an expert resume parser. Analyze this resume image and extract ALL information into JSON.

RULES:
1. Read EVERY section carefully - do not skip anything
2. Dates: use YYYY-MM format (e.g., 2023-06)
3. Current positions: endDate = null, isCurrent = true
4. Skill levels based on experience: EXPERT (5+ yrs), ADVANCED (3-5 yrs), INTERMEDIATE (1-3 yrs), BEGINNER (<1 yr)
5. Keep original language (Chinese/English)
6. If not found, use "" or []

OUTPUT (pure JSON only, NO markdown, NO explanation):
{"name": "", "email": "", "phone": "", "linkedin": "", "github": "", "website": "", "location": "", "headline": "", "summary": "", "work_experience": [{"company": "", "title": "", "location": "", "employment_type": "FULL_TIME", "start_date": "", "end_date": "", "is_current": false, "description": "", "achievements": [], "technologies": []}], "education": [{"school": "", "degree": "", "field": "", "location": "", "start_date": "", "end_date": "", "gpa": "", "description": "", "activities": [], "honors": []}], "projects": [{"name": "", "description": "", "url": "", "repo_url": "", "technologies": [], "start_date": "", "end_date": "", "highlights": []}], "skills": [{"name": "", "level": "INTERMEDIATE", "category": ""}], "certifications": [{"name": "", "issuer": "", "date": "", "url": ""}], "languages": [{"language": "", "proficiency": ""}], "achievements": []}"""


class ResumeParser:
    """
    简历解析器
    
    功能：
    - 从 PDF 提取文本并解析
    - 从图像 OCR 提取并解析
    - 使用 LLM/VLM 进行结构化解析
    - 支持多页 PDF 处理
    """

    def __init__(
        self, 
        llm_client: Optional[LLMClient] = None,
        vision_client: Optional[VisionClient] = None
    ):
        self.llm = llm_client
        self.vision = vision_client

    def parse_pdf_bytes(self, pdf_bytes: bytes) -> ParsedResume:
        """
        解析 PDF 字节数据
        
        Args:
            pdf_bytes: PDF 文件的字节内容
            
        Returns:
            解析后的简历数据
        """
        # 优先使用 VLM 解析 PDF（更快更准确）
        if self.vision:
            return self._parse_pdf_as_image(pdf_bytes)
        
        # 如果没有 VLM，回退到 LLM 解析
        raw_text = self._extract_text_from_pdf_bytes(pdf_bytes)
        if not raw_text.strip() or len(raw_text.strip()) < 50:
            return ParsedResume(raw_text="[无法提取文本，需要视觉模型]")
        
        return self._parse_with_llm(raw_text)

    def parse_pdf(self, pdf_path: str) -> ParsedResume:
        """解析 PDF 文件路径"""
        with open(pdf_path, 'rb') as f:
            return self.parse_pdf_bytes(f.read())

    def parse_image(self, image_data: Union[str, bytes]) -> ParsedResume:
        """
        解析图像简历
        
        Args:
            image_data: 图像 URL、base64 或字节数据
        """
        if not self.vision:
            return ParsedResume(raw_text="[需要视觉模型进行图像解析]")
        
        response = self.vision.analyze_image(image_data, VISION_PARSE_PROMPT)
        return self._parse_json_response(response, "[图像简历]")

    def parse_text(self, text: str) -> ParsedResume:
        """直接解析文本"""
        return self._parse_with_llm(text)

    def _extract_text_from_pdf_bytes(self, pdf_bytes: bytes) -> str:
        """从 PDF 字节提取文本"""
        try:
            reader = PdfReader(io.BytesIO(pdf_bytes))
            text_parts = []
            for page in reader.pages:
                page_text = page.extract_text()
                if page_text:
                    text_parts.append(page_text)
            return "\n".join(text_parts)
        except Exception as e:
            return f"[PDF解析错误: {str(e)}]"

    def _parse_pdf_as_image(self, pdf_bytes: bytes) -> ParsedResume:
        """将 PDF 转为图像进行 VLM 解析"""
        import logging
        logger = logging.getLogger(__name__)
        
        try:
            from pdf2image import convert_from_bytes
            import base64
            
            # 转换前3页
            logger.info(f"Converting PDF to images, size={len(pdf_bytes)} bytes")
            images = convert_from_bytes(pdf_bytes, first_page=1, last_page=3, dpi=150)
            logger.info(f"Converted to {len(images)} images")
            
            all_results = []
            for i, img in enumerate(images):
                # 转为 base64
                buffer = io.BytesIO()
                img.save(buffer, format='PNG')
                img_bytes = buffer.getvalue()
                img_b64 = base64.b64encode(img_bytes).decode()
                logger.info(f"Page {i+1}: image size={len(img_bytes)} bytes")
                
                # 用 VLM 分析
                response = self.vision.analyze_image(
                    f"data:image/png;base64,{img_b64}",
                    VISION_PARSE_PROMPT
                )
                logger.info(f"Page {i+1} VLM response length: {len(response) if response else 0}")
                if response:
                    logger.info(f"Page {i+1} VLM response preview: {response[:500]}")
                all_results.append(response)
            
            # 合并解析结果 (取第一页为主，后续页补充)
            if all_results:
                merged = self._parse_json_response(all_results[0], "[PDF图像]")
                logger.info(f"Parsed result: name={merged.name}, skills={len(merged.skills)}, exp={len(merged.work_experience)}")
                return merged
            
            return ParsedResume(raw_text="[PDF图像解析失败]")
            
        except ImportError as e:
            logger.error(f"Import error: {e}")
            return ParsedResume(raw_text="[需要安装 pdf2image 库]")
        except Exception as e:
            logger.error(f"PDF image parse error: {e}", exc_info=True)
            return ParsedResume(raw_text=f"[PDF图像解析错误: {str(e)}]")

    def _parse_with_llm(self, text: str) -> ParsedResume:
        """使用 LLM 解析简历文本"""
        if not self.llm:
            return ParsedResume(raw_text=text)
        
        # 限制文本长度，避免超出 token 限制
        truncated_text = text[:12000]
        # 使用 replace 而不是 .format() 因为 prompt 中有 JSON 花括号
        prompt = RESUME_PARSE_PROMPT.replace("{resume_text}", truncated_text)
        
        messages = [
            {"role": "system", "content": "你是一个专业的简历解析助手。请严格按照要求的 JSON 格式输出，确保所有字段都存在。"},
            {"role": "user", "content": prompt}
        ]
        
        try:
            response = self.llm.chat(messages, temperature=0.1, max_tokens=4096)
            return self._parse_json_response(response, text)
        except Exception as e:
            return ParsedResume(raw_text=text, summary=f"[LLM解析错误: {str(e)}]")

    def _parse_json_response(self, response: str, raw_text: str) -> ParsedResume:
        """解析 LLM 返回的 JSON"""
        try:
            # 尝试提取 JSON
            json_str = response
            if "```json" in response:
                json_str = response.split("```json")[1].split("```")[0]
            elif "```" in response:
                json_str = response.split("```")[1].split("```")[0]
            
            data = json.loads(json_str.strip())
            
            # 处理技能 - 确保格式正确
            skills = data.get("skills", [])
            if skills and isinstance(skills[0], str):
                # 如果只是字符串列表，转换为对象格式
                skills = [{"name": s, "level": "INTERMEDIATE", "category": "其他"} for s in skills]
            
            return ParsedResume(
                raw_text=raw_text,
                name=data.get("name", ""),
                email=data.get("email", ""),
                phone=data.get("phone", ""),
                linkedin=data.get("linkedin", ""),
                github=data.get("github", ""),
                website=data.get("website", ""),
                location=data.get("location", ""),
                headline=data.get("headline", ""),
                summary=data.get("summary", ""),
                work_experience=data.get("work_experience", []),
                education=data.get("education", []),
                projects=data.get("projects", []),
                skills=skills,
                certifications=data.get("certifications", []),
                achievements=data.get("achievements", []),
                languages=data.get("languages", []),
            )
        except json.JSONDecodeError as e:
            return ParsedResume(raw_text=raw_text, summary=f"[JSON解析失败: {str(e)}]")

    def _normalize_date(self, date_str: str) -> str:
        """标准化日期格式为 YYYY-MM"""
        if not date_str:
            return ""
        
        date_str = date_str.strip().lower()
        
        # 处理 "present", "至今", "现在" 等
        if date_str in ["present", "至今", "现在", "now", "current"]:
            return "present"
        
        # 尝试匹配 YYYY-MM 格式
        match = re.match(r'(\d{4})[-/.]?(\d{1,2})?', date_str)
        if match:
            year = match.group(1)
            month = match.group(2) or "01"
            return f"{year}-{month.zfill(2)}"
        
        return date_str
