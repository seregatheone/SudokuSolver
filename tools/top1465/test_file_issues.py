from __future__ import annotations

import importlib.util
from pathlib import Path
import unittest


MODULE_PATH = Path(__file__).with_name("file_issues.py")
SPEC = importlib.util.spec_from_file_location("top1465_file_issues", MODULE_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"Could not load {MODULE_PATH}")
FILE_ISSUES = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(FILE_ISSUES)


class ValidatedIssueUrlTest(unittest.TestCase):
    def test_accepts_issue_url_for_current_repo(self) -> None:
        url = "https://github.com/seregatheone/SudokuSolver/issues/519"

        actual = FILE_ISSUES.validated_issue_url(
            result={"url": url},
            repo="seregatheone/SudokuSolver",
            line_number=692,
            title="top1465 #0692: логический решатель останавливается",
        )

        self.assertEqual(url, actual)

    def test_rejects_empty_issue_url_with_puzzle_context(self) -> None:
        with self.assertRaisesRegex(RuntimeError, r"line 692.*#0692.*None"):
            FILE_ISSUES.validated_issue_url(
                result={"url": None},
                repo="seregatheone/SudokuSolver",
                line_number=692,
                title="top1465 #0692: логический решатель останавливается",
            )

    def test_rejects_issue_url_for_another_repo(self) -> None:
        with self.assertRaisesRegex(RuntimeError, "invalid URL"):
            FILE_ISSUES.validated_issue_url(
                result={"url": "https://github.com/example/other/issues/1"},
                repo="seregatheone/SudokuSolver",
                line_number=692,
                title="top1465 #0692: логический решатель останавливается",
            )


class LineNumberFromIssueTitleTest(unittest.TestCase):
    def test_returns_same_line_for_changed_outcome_title(self) -> None:
        self.assertEqual(
            240,
            FILE_ISSUES.line_number_from_issue_title(
                "top1465 #0240: логический решатель повреждает решение",
            ),
        )
        self.assertEqual(
            240,
            FILE_ISSUES.line_number_from_issue_title(
                "top1465 #0240: логический решатель останавливается",
            ),
        )

    def test_ignores_unrelated_issue_title(self) -> None:
        self.assertIsNone(FILE_ISSUES.line_number_from_issue_title("UI polish"))


if __name__ == "__main__":
    unittest.main()
