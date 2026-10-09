document.addEventListener('DOMContentLoaded', () => {

  const transfers =
    JSON.parse(localStorage.getItem('transactionHistory')) || [];

  /* =========================
     HELPERS
  ========================== */

  function truncateText(text, maxLength = 27) {
    return text.length > maxLength
      ? text.slice(0, maxLength) + '...'
      : text;
  }

  function formatMoney(value) {
    return Number(value || 0).toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  }

  const MONTHS = ['Jan','Feb','Mar','Apr','May','Jun',
                  'Jul','Aug','Sep','Oct','Nov','Dec'];

  /* =========================
     POPULATE DROPDOWNS
  ========================== */

  const selMonth = document.querySelector('#selMonth');
  const selYear  = document.querySelector('#selYear');

  const now = new Date();
  const curMonth = now.getMonth();          // 0-based
  const curYear  = now.getFullYear();

  // Populate month options
  MONTHS.forEach((m, i) => {
    const o = document.createElement('option');
    o.value = String(i);
    o.textContent = m;
    selMonth.appendChild(o);
  });

  // Build year list: current year ± 5, plus any year present in txns
  const years = new Set();
  const FROM = curYear - 5;
  const TO   = curYear + 1;
  for (let y = TO; y >= FROM; y--) years.add(y);
  transfers.forEach(t => {
    if (!t.timestamp) return;
    const d = new Date(t.timestamp);
    if (isNaN(d.getTime())) return;
    years.add(d.getFullYear());
  });
  const sortedYears = Array.from(years).sort((a, b) => b - a);

  sortedYears.forEach(y => {
    const o = document.createElement('option');
    o.value = String(y);
    o.textContent = String(y);
    selYear.appendChild(o);
  });

  // Default selection: current month + current year
  selMonth.value = String(curMonth);
  selYear.value  = String(curYear);

  /* =========================
     RENDER
  ========================== */

  const container = document.querySelector('#history');

  function render() {
    if (!container) return;

    const mSel = parseInt(selMonth.value, 10);
    const ySel = parseInt(selYear.value, 10);

    const filtered = transfers.filter(t => {
      if (!t.timestamp) return false;
      const d = new Date(t.timestamp);
      if (isNaN(d.getTime())) return false;
      return d.getMonth() === mSel && d.getFullYear() === ySel;
    });

    // Totals
    let inSum = 0, outSum = 0;
    filtered.forEach(t => {
      const amt = Number(t.amount || 0);
      const isDeposit = (t.type || '').toLowerCase() === 'deposit';
      if (isDeposit) inSum += amt; else outSum += amt;
    });

    // Update In / Out labels
    const transIn  = document.querySelector('#transIn');
    const transOut = document.querySelector('#transOut');
    if (transIn)  transIn.textContent  = `₦${formatMoney(inSum)}`;
    if (transOut) transOut.textContent = `₦${formatMoney(outSum)}`;

    if (filtered.length === 0) {
      container.innerHTML = `<div class="mini-empty">No transactions in ${MONTHS[mSel]} ${ySel}</div>`;
      return;
    }

    // Sort newest first
    const list = filtered.slice().sort((a, b) => {
      const da = a.timestamp ? new Date(a.timestamp).getTime() : 0;
      const db = b.timestamp ? new Date(b.timestamp).getTime() : 0;
      return db - da;
    });

    container.innerHTML = list.map((t) => {
      const isDeposit = (t.type || '').toLowerCase() === 'deposit';

      const text = isDeposit
        ? truncateText(`Transfer from ${t.senderName || 'Admin'}`)
        : truncateText(`Transfer to ${t.recipient || 'Unknown'}`);

      const amount = Number(t.amount || 0);

      const arrowSvg = isDeposit
        ? '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#00A86B" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12l7 7 7-7"/></svg>'
        : '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#00A86B" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 19V5M5 12l7-7 7 7"/></svg>';

      const globalIndex = transfers.indexOf(t);
      return `
        <div class="mini-flex transaction-item" data-index="${globalIndex}">
          <div class="mini-flex-left">
            <div class="mini-arrow-circle">${arrowSvg}</div>
            <div>
              <div class="mini-text">${text}</div>
              <div class="mini-date">${t.date || ''}</div>
            </div>
          </div>
          <div class="mini-amount-cover">
            <div class="${isDeposit ? 'mini-trans-amount-plus' : 'mini-trans-amount'}">
              ${isDeposit ? '+' : '-'}₦${formatMoney(amount)}
            </div>
            <div class="status">Successful</div>
          </div>
        </div>
      `;
    }).join('');

    // Re-bind click -> receipt
    container.querySelectorAll('.transaction-item').forEach(item => {
      item.addEventListener('click', () => {
        const idx = item.dataset.index;
        if (window.Android && typeof window.Android.openReceipt === 'function') {
          window.Android.openReceipt(String(idx));
        }
      });
    });
  }

  // React to dropdown changes
  selMonth.addEventListener('change', render);
  selYear.addEventListener('change', render);

  // First paint
  render();

});