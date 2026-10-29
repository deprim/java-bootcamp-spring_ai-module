const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

function browser() {
    const elements = new Map();
    const context = vm.createContext({
        document: {
            getElementById(id) {
                if (!elements.has(id)) elements.set(id, {
                    value: '', disabled: false, textContent: '', innerHTML: '',
                    addEventListener() {}, focus() {}, appendChild() {},
                    querySelector() { return null; }
                });
                return elements.get(id);
            }
        },
        fetch: async () => ({ ok: true, json: async () => [] })
    });
    vm.runInContext(fs.readFileSync('src/main/resources/static/app.js', 'utf8'), context);
    vm.runInContext(`
        renderMessages = chat => { rendered = chat.id; };
        appendPending = () => {};
        refreshChatList = async () => {};
        api.get = async id => ({id});
        api.send = () => new Promise((resolve, reject) => { resolveSend = resolve; rejectSend = reject; });
        activeChatId = 'chat-A';
    `, context);
    return { context, elements, run: code => vm.runInContext(code, context) };
}

test('a late model response cannot replace the selected conversation', async () => {
    const b = browser();
    b.run("submission = submitMessage('Hello A')");
    await b.run("openChat('chat-B')");
    b.run("resolveSend({id: 'chat-A'})");
    await b.run('submission');
    assert.equal(b.run('activeChatId'), 'chat-B');
    assert.equal(b.run('rendered'), 'chat-B');
});

test('deleting the active conversation during generation leaves the composer disabled', async () => {
    const b = browser();
    b.run("api.remove = async () => {}; submission = submitMessage('Hello A')");
    await b.run("deleteChat('chat-A')");
    b.run("resolveSend({id: 'chat-A'})");
    await b.run('submission');
    assert.equal(b.run('activeChatId'), null);
    assert.equal(b.elements.get('input').disabled, true);
});

test('failed generation restores the draft in the same conversation', async () => {
    const b = browser();
    b.run("submission = submitMessage('Keep this question'); rejectSend(new Error('Ollama is unavailable'))");
    await b.run('submission');
    assert.equal(b.elements.get('input').value, 'Keep this question');
    assert.equal(b.elements.get('error').textContent, 'Ollama is unavailable');
});

test('an earlier conversation fetch cannot replace a newer selection', async () => {
    const b = browser();
    b.run("api.get = id => id === 'chat-A' ? new Promise(resolve => { resolveGet = resolve; }) : Promise.resolve({id})");
    b.run("opening = openChat('chat-A')");
    await b.run("openChat('chat-B')");
    b.run("resolveGet({id: 'chat-A'})");
    await b.run('opening');
    assert.equal(b.run('rendered'), 'chat-B');
});
