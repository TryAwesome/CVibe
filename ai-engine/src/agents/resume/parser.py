"""
PDF Resume Parser - 从 PDF 简历提取结构化数据
================================================================================

当用户没有进行过 Interview 时，会使用最后上传的 PDF 简历来构建个人资料库。
"""

from typing import Optional
from dataclasses import dataclass, field

# from PyPDF2 import PdfReader  # 实际使用时取消注释

from ..llm import LLMClient


@dataclass
class ParsedResume:
    """解析后的简历数据"""
    raw_text: str = ""
    name: str = ""
    email: str = ""
    phone: str = ""
    education: list[dict] = field(default_factory=list)
    work_experience: list[dict] = field(default_factory=list)
    projects: list[dict] = field(default_factory=list)
    skills: list[str] = field(default_factory=list)
    achievements: list[str] = field(default_factory=list)
    summary: str = ""


class ResumeParser:
    """
    PDF 简历解析器
    
    功能：
    - 从 PDF 提取文本
    - 使用 LLM 进行结构化解析
    - 输出可用于个人资料库的数据
    """

    SYSTEM_PROMPT = """你是一个简历解析专家。请从以下简历文本中提取结构化信息。

请提取以下字段：
1. 基本信息：姓名、邮箱、电话
2. 教育背景：学校、学位、专业、时间、GPA
3. 工作经历：公司、职位、时间、职责描述
4. 项目经历：项目名、技术栈、描述、成果
5. 技能：编程语言、框架、工具
6. 获奖/证书

简历文本：
{resume_text}

请以 JSON 格式输出。
"""

    def __init__(self, llm_client: Optional[LLMClient] = None):
        self.llm = llm_client

    def parse_pdf(self, pdf_path: str) -> ParsedResume:
        """
        解析 PDF 简历
        
        Args:
            pdf_path: PDF 文件路径
            
        Returns:
            解析后的简历数据
        """
        # 1. 提取 PDF 文本
        raw_text = self._extract_text_from_pdf(pdf_path)
        
        # 2. 使用 LLM 解析
        parsed = self._parse_with_llm(raw_text)
        
        return parsed

    def _extract_text_from_pdf(self, pdf_path: str) -> str:
        """从 PDF 提取文本"""
        # 占位实现
        # TODO: 实际使用 PyPDF2 或 pdfplumber
        # reader = PdfReader(pdf_path)
        # text = ""
        # for page in reader.pages:
        #     text += page.extract_text()
        # return text
        return "PDF text placeholder"

    def _parse_with_llm(self, text: str) -> ParsedResume:
        """使用 LLM 解析简历文本"""
        # 占位实现
        # TODO: 调用 LLM 进行结构化解析
        return ParsedResume(
            raw_text=text,
            name="",
            email="",
            phone="",
            education=[],
            work_experience=[],
            projects=[],
            skills=[],
            achievements=[],
        )

    def parse_text(self, text: str) -> ParsedResume:
        """直接解析文本（用于非 PDF 输入）"""
        return self._parse_with_llm(text)
