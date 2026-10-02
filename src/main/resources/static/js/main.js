// Theme Toggle Logic
document.addEventListener('DOMContentLoaded', () => {
    const themeToggleBtn = document.getElementById('theme-toggle');
    const sunIcon = document.getElementById('sun-icon');
    const moonIcon = document.getElementById('moon-icon');

    const currentTheme = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-theme', currentTheme);
    updateThemeIcons(currentTheme);

    if (themeToggleBtn) {
        themeToggleBtn.addEventListener('click', () => {
            const theme = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
            document.documentElement.setAttribute('data-theme', theme);
            localStorage.setItem('theme', theme);
            updateThemeIcons(theme);
            // Reload page to re-render charts with correct theme colors if needed
            if (window.location.pathname.includes('/report') || window.location.pathname.includes('/dashboard')) {
                window.location.reload();
            }
        });
    }

    function updateThemeIcons(theme) {
        if (!sunIcon || !moonIcon) return;
        if (theme === 'dark') {
            sunIcon.classList.add('d-none');
            moonIcon.classList.remove('d-none');
        } else {
            sunIcon.classList.remove('d-none');
            moonIcon.classList.add('d-none');
        }
    }

    // File Drag and Drop Visuals
    const dropzone = document.getElementById('dropzone');
    const fileInput = document.getElementById('file-input');

    if (dropzone && fileInput) {
        dropzone.addEventListener('click', () => fileInput.click());
        dropzone.addEventListener('dragover', (e) => { e.preventDefault(); dropzone.classList.add('dragover'); });
        ['dragleave', 'drop'].forEach(eventName => dropzone.addEventListener(eventName, () => dropzone.classList.remove('dragover')));
        dropzone.addEventListener('drop', (e) => {
            e.preventDefault();
            if (e.dataTransfer.files.length) {
                fileInput.files = e.dataTransfer.files;
                updateFileNameDisplay(e.dataTransfer.files[0].name);
            }
        });
        fileInput.addEventListener('change', () => {
            if (fileInput.files.length) updateFileNameDisplay(fileInput.files[0].name);
        });
    }

    function updateFileNameDisplay(name) {
        const textElement = document.getElementById('upload-text');
        if (textElement) {
            textElement.innerHTML = `<i class="bi bi-file-earmark-check fs-1 text-success"></i><br><strong>${name}</strong> selected.<br><span class="text-muted">Click or drag another to replace</span>`;
        }
    }
});

// Chart.js Global Helpers
const isDarkTheme = () => document.documentElement.getAttribute('data-theme') === 'dark';
const getTextColor = () => isDarkTheme() ? '#cbd5e1' : '#475569';
const getGridColor = () => isDarkTheme() ? '#1e293b' : '#e2e8f0';

// 1. Radar Chart (Score Breakdown)
const tooltipDetails = {
    'Keywords': {
        reason: 'Matches core terms from the target job profile.',
        tip: 'Ensure missing keywords identified in the analysis are added.'
    },
    'Skills': {
        reason: 'Identifies specific technical stack matches.',
        tip: 'List matching skills in a dedicated, clearly titled section.'
    },
    'Experience': {
        reason: 'Checks work history dates and active role structures.',
        tip: 'Use robust action verbs and quantify achievements.'
    },
    'Projects': {
        reason: 'Assesses project complexity and technical depth.',
        tip: 'Add measurable details, tech tags, and github links.'
    },
    'Education': {
        reason: 'Matches academic degrees and date structures.',
        tip: 'Ensure degree name, major, and graduation year are clear.'
    },
    'Format': {
        reason: 'Checks formatting, fonts, and link alignments.',
        tip: 'Avoid complex double-column styles and floating text boxes.'
    },
    'Grammar': {
        reason: 'Validates spelling, phrasing, and verb usage.',
        tip: 'Ensure bullet points are concise and begin with action verbs.'
    },
    'Internship': {
        reason: 'Evaluates professional internship experience.',
        tip: 'Explicitly describe responsibilities and project outputs.'
    },
    'Certifications': {
        reason: 'Matches target professional credentials.',
        tip: 'List certifications with names, dates, and issuers.'
    }
};

function initializeRadarChart(scores) {
    const ctx = document.getElementById('radarChart');
    if (!ctx) return;
    new Chart(ctx, {
        type: 'radar',
        data: {
            labels: ['Keywords', 'Skills', 'Experience', 'Projects', 'Education', 'Format', 'Grammar', 'Internship', 'Certifications'],
            datasets: [{
                label: 'Score Mapping',
                data: [scores.keyword, scores.skill, scores.experience, scores.project, scores.education, scores.format, scores.grammar, scores.internship, scores.certifications],
                backgroundColor: 'rgba(99, 102, 241, 0.2)',
                borderColor: '#6366f1',
                pointBackgroundColor: '#0ea5e9',
                pointBorderColor: '#fff',
                pointHoverBackgroundColor: '#fff',
                pointHoverBorderColor: '#0ea5e9'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                r: {
                    angleLines: { color: getGridColor() },
                    grid: { color: getGridColor() },
                    pointLabels: { color: getTextColor(), font: { size: 12 } },
                    ticks: { color: getTextColor(), backdropColor: 'transparent', min: 0, max: 100, stepSize: 20 }
                }
            },
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.raw || 0;
                            const details = tooltipDetails[label] || { reason: '', tip: '' };
                            return [
                                `${label}: ${value}%`,
                                `Maximum Score: 100%`,
                                `Reason: ${details.reason}`,
                                `Improvement Tip: ${details.tip}`
                            ];
                        }
                    }
                }
            }
        }
    });
}

// 2. Trend Line Chart (Dashboard)
function initializeTrendChart(dates, scores) {
    const ctx = document.getElementById('trendChart');
    if (!ctx) return;
    new Chart(ctx, {
        type: 'line',
        data: {
            labels: dates,
            datasets: [{
                label: 'ATS Overall Score',
                data: scores,
                borderColor: '#4338ca',
                backgroundColor: 'rgba(67, 56, 202, 0.1)',
                borderWidth: 3,
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: { min: 0, max: 100, grid: { color: getGridColor() }, ticks: { color: getTextColor() } },
                x: { grid: { display: false }, ticks: { color: getTextColor() } }
            },
            plugins: { legend: { display: false } }
        }
    });
}

// --- AI AJAX Functions --- //
async function callAiSummary() {
    const btn = document.getElementById('btnAiSummary');
    const resultBox = document.getElementById('aiSummaryResult');
    btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Generating...';
    btn.disabled = true;

    try {
        let res = await fetch('/resume/api/ai/summary', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ jobTitle: 'Software Professional', experience: '5 years', skills: 'Java, Cloud' })
        });
        let data = await res.json();
        resultBox.innerHTML = `<strong>Suggested Summary:</strong><br><p class="mt-2 text-primary-custom">${data.result}</p>`;
    } catch (e) {
        resultBox.innerHTML = `<span class="text-danger">Failed to reach AI Engine.</span>`;
    } finally {
        btn.innerHTML = '<i class="bi bi-magic"></i> Generate Summary';
        btn.disabled = false;
    }
}

async function callAiRewrite() {
    const btn = document.getElementById('btnAiRewrite');
    const input = document.getElementById('aiRewriteInput').value;
    const resultBox = document.getElementById('aiRewriteResult');
    
    if(!input) { resultBox.innerHTML = `<span class="text-warning">Please enter a description first.</span>`; return; }

    btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Rewriting...';
    btn.disabled = true;

    try {
        let res = await fetch('/resume/api/ai/rewrite', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: input })
        });
        let data = await res.json();
        resultBox.innerHTML = `<strong>Optimized Description:</strong><br><p class="mt-2 text-primary-custom">${data.result}</p>`;
    } catch (e) {
        resultBox.innerHTML = `<span class="text-danger">Failed to reach AI Engine.</span>`;
    } finally {
        btn.innerHTML = '<i class="bi bi-stars"></i> Rewrite with AI';
        btn.disabled = false;
    }
}

// PDF Download function using html2pdf
function downloadPDF() {
    const element = document.getElementById('reportContent');
    if (!element) return;
    
    // Hide buttons temporarily during PDF generation
    const buttons = document.querySelectorAll('.no-print');
    buttons.forEach(btn => btn.style.display = 'none');
    
    const opt = {
        margin:       1,
        filename:     'CareerBoost_Resume_Report.pdf',
        image:        { type: 'jpeg', quality: 0.98 },
        html2canvas:  { scale: 2, useCORS: true },
        jsPDF:        { unit: 'in', format: 'letter', orientation: 'portrait' }
    };
    
    html2pdf().set(opt).from(element).save().then(() => {
        // Restore buttons
        buttons.forEach(btn => btn.style.display = '');
    });
}
