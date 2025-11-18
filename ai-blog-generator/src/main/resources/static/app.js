let isGenerating = false;

document.addEventListener('DOMContentLoaded', function() {
    const generateBtn = document.getElementById('generateBtn');
    const stopBtn = document.getElementById('stopBtn');
    const promptInput = document.getElementById('prompt');
    const articleCountInput = document.getElementById('articleCount');
    const delaySecondsInput = document.getElementById('delaySeconds');
    const autoSaveCheckbox = document.getElementById('autoSave');

    generateBtn.addEventListener('click', handleGenerate);
    stopBtn.addEventListener('click', handleStop);
});

async function handleGenerate() {
    const prompt = document.getElementById('prompt').value.trim();
    const articleCount = parseInt(document.getElementById('articleCount').value);
    const delaySeconds = parseInt(document.getElementById('delaySeconds').value);
    const autoSave = document.getElementById('autoSave').checked;

    if (!prompt) {
        alert('프롬프트를 입력해주세요.');
        return;
    }

    if (articleCount < 1 || articleCount > 10) {
        alert('생성할 원고 수는 1~10 사이로 설정해주세요.');
        return;
    }

    isGenerating = true;
    updateButtonStates();

    showProgressSection();
    clearResults();

    const requestData = {
        prompt: prompt,
        articleCount: articleCount,
        delaySeconds: delaySeconds,
        autoSave: autoSave
    };

    try {
        const response = await fetch('/api/generate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        });

        const results = await response.json();

        displayResults(results);

    } catch (error) {
        console.error('오류 발생:', error);
        alert('원고 생성 중 오류가 발생했습니다: ' + error.message);
    } finally {
        isGenerating = false;
        updateButtonStates();
        hideProgressSection();
    }
}

function handleStop() {
    isGenerating = false;
    updateButtonStates();
    alert('작업이 중단되었습니다.');
}

function updateButtonStates() {
    const generateBtn = document.getElementById('generateBtn');
    const stopBtn = document.getElementById('stopBtn');

    generateBtn.disabled = isGenerating;
    stopBtn.disabled = !isGenerating;
}

function showProgressSection() {
    const progressSection = document.getElementById('progressSection');
    progressSection.style.display = 'block';
    updateProgress(0, parseInt(document.getElementById('articleCount').value));
}

function hideProgressSection() {
    const progressSection = document.getElementById('progressSection');
    const progressBar = document.getElementById('progressBar');
    progressBar.style.width = '100%';
}

function updateProgress(current, total) {
    const progressBar = document.getElementById('progressBar');
    const progressText = document.getElementById('progressText');

    const percentage = (current / total) * 100;
    progressBar.style.width = percentage + '%';
    progressText.textContent = `${current} / ${total}`;
}

function clearResults() {
    const resultsDiv = document.getElementById('results');
    resultsDiv.innerHTML = '';
    const resultsSection = document.getElementById('resultsSection');
    resultsSection.style.display = 'none';
}

function displayResults(results) {
    const resultsSection = document.getElementById('resultsSection');
    const resultsDiv = document.getElementById('results');

    resultsSection.style.display = 'block';
    resultsDiv.innerHTML = '';

    results.forEach((result, index) => {
        const resultElement = createResultElement(result, index);
        resultsDiv.appendChild(resultElement);
        updateProgress(index + 1, results.length);
    });
}

function createResultElement(result, index) {
    const div = document.createElement('div');
    div.className = `result-item ${result.success ? 'success' : 'error'}`;

    const header = document.createElement('div');
    header.className = 'result-header';

    const title = document.createElement('div');
    title.className = 'result-title';
    title.textContent = `원고 ${result.articleNumber}`;

    const status = document.createElement('span');
    status.className = `result-status ${result.success ? 'success' : 'error'}`;
    status.textContent = result.success ? '성공' : '실패';

    header.appendChild(title);
    header.appendChild(status);
    div.appendChild(header);

    if (!result.success) {
        const errorMsg = document.createElement('p');
        errorMsg.style.color = '#dc3545';
        errorMsg.style.marginTop = '10px';
        errorMsg.textContent = result.message;
        div.appendChild(errorMsg);
        return div;
    }

    const contentDiv = document.createElement('div');
    contentDiv.className = 'result-content';

    const originalBlock = document.createElement('div');
    originalBlock.className = 'content-block';
    originalBlock.innerHTML = `
        <h4>원본 원고 (Claude AI)</h4>
        <pre>${escapeHtml(result.originalContent)}</pre>
    `;
    contentDiv.appendChild(originalBlock);

    const replacedBlock = document.createElement('div');
    replacedBlock.className = 'content-block';
    replacedBlock.innerHTML = `
        <h4>치환된 원고 (ChatGPT)</h4>
        <pre>${escapeHtml(result.replacedContent)}</pre>
    `;
    contentDiv.appendChild(replacedBlock);

    div.appendChild(contentDiv);

    if (result.savedFilePath) {
        const filePath = document.createElement('div');
        filePath.className = 'file-path';
        filePath.textContent = `저장됨: ${result.savedFilePath}`;
        div.appendChild(filePath);
    }

    const actions = document.createElement('div');
    actions.className = 'result-actions';

    const copyOriginalBtn = document.createElement('button');
    copyOriginalBtn.className = 'btn-small btn-copy';
    copyOriginalBtn.textContent = '원본 복사';
    copyOriginalBtn.onclick = () => copyToClipboard(result.originalContent);

    const copyReplacedBtn = document.createElement('button');
    copyReplacedBtn.className = 'btn-small btn-copy';
    copyReplacedBtn.textContent = '치환본 복사';
    copyReplacedBtn.onclick = () => copyToClipboard(result.replacedContent);

    actions.appendChild(copyOriginalBtn);
    actions.appendChild(copyReplacedBtn);
    div.appendChild(actions);

    return div;
}

function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        alert('클립보드에 복사되었습니다.');
    }).catch(err => {
        console.error('복사 실패:', err);
        alert('복사에 실패했습니다.');
    });
}
