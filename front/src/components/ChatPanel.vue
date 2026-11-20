<template>
    <div v-if="chatStore.open" class="chat-backdrop" @click.self="closeChat">
        <section class="chat-panel">
            <header class="chat-header">
                <span class="chat-title">{{ t('chat') }}</span>
                <button class="chat-close" type="button" :aria-label="t('close')" @click="closeChat">×</button>
            </header>

            <nav class="chat-tabs">
                <button type="button" :class="{ active: activeTab === 'lobby' }" @click="selectLobby">
                    {{ t('lobby') }}
                </button>
                <button v-if="gameRoom" type="button" :class="{ active: activeTab === 'game' }" @click="selectGame">
                    {{ t('game') }}
                </button>
            </nav>

            <div ref="listEl" class="chat-messages" @scroll="onScroll">
                <button v-if="room.hasMore" type="button" class="chat-load-older" :disabled="room.loading"
                    @click="loadOlder(roomId)">
                    {{ t('loadOlder') }}
                </button>

                <div v-if="room.loading && messages.length === 0" class="chat-empty">{{ t('loading') }}</div>
                <div v-else-if="messages.length === 0" class="chat-empty">{{ t('noMessages') }}</div>

                <template v-for="message in messages" :key="message.id">
                    <div v-if="message.kind === 'SYSTEM'" class="chat-system">{{ message.text }}</div>
                    <div v-else class="chat-message" :class="{ self: isSelf(message), pending: message.id.startsWith('pending:') }">
                        <div class="chat-avatar" :style="{ backgroundColor: message.userColor }"></div>
                        <div class="chat-content">
                            <div class="chat-meta">
                                <span class="chat-name">{{ message.userName }}</span>
                                <time>{{ formatTime(message.createdAt) }}</time>
                            </div>
                            <div class="chat-text">{{ message.text }}</div>
                        </div>
                    </div>
                </template>
            </div>

            <div v-if="room.error" class="chat-error">{{ room.error }}</div>

            <div class="chat-compose">
                <textarea v-model="draft" class="chat-input" rows="1" :maxlength="maxLength"
                    :placeholder="t('placeholder')" @keydown="onKeydown"></textarea>
                <button class="chat-send" type="button" :disabled="!canSend" @click="send">{{ t('send') }}</button>
            </div>
            <div v-if="draft.length > 1800" class="chat-counter">{{ draft.length }}/{{ maxLength }}</div>
        </section>
    </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ConnectStatus, type ChatMessage } from 'boardgame-web-common/back'
import { useLocalStore, useMemoryLocalStore } from '../services/localStore'
import { useChatStore } from '../services/chatStore'
import { LOBBY_ROOM_ID, closeChat, gameRoomId, loadOlder, sendChat, setActiveRoom } from '../services/chatService'

const maxLength = 2000

const { t, locale } = useI18n({
    messages: {
        en: {
            chat: 'Chat',
            close: 'Close',
            lobby: 'Lobby',
            game: 'Game',
            loadOlder: 'Load older',
            loading: 'Loading…',
            noMessages: 'No messages yet',
            placeholder: 'Write a message…',
            send: 'Send'
        },
        ru: {
            chat: 'Чат',
            close: 'Закрыть',
            lobby: 'Лобби',
            game: 'Игра',
            loadOlder: 'Загрузить старые',
            loading: 'Загрузка…',
            noMessages: 'Сообщений пока нет',
            placeholder: 'Написать сообщение…',
            send: 'Отправить'
        }
    }
})

const route = useRoute()
const localStore = useLocalStore()
const memoryLocalStore = useMemoryLocalStore()
const chatStore = useChatStore()

const draft = ref('')
const listEl = ref<HTMLElement | null>(null)
const atBottom = ref(true)

const gameId = computed(() => route.name === 'game' ? String(route.params.id) : undefined)
const gameRoom = computed(() => gameId.value ? gameRoomId(gameId.value) : undefined)
const roomId = computed(() => chatStore.activeRoomId)
const room = computed(() => chatStore.room(roomId.value))
const messages = computed(() => room.value.messages.filter(message => !message.deleted))
const activeTab = computed(() => roomId.value === gameRoom.value ? 'game' : 'lobby')
const connected = computed(() => memoryLocalStore.connectStatus === ConnectStatus.CONNECTED)
const canSend = computed(() => connected.value && draft.value.trim().length > 0 && draft.value.length <= maxLength)

function selectDefaultRoom() {
    setActiveRoom(gameRoom.value ?? LOBBY_ROOM_ID)
}

function selectLobby() {
    setActiveRoom(LOBBY_ROOM_ID)
}

function selectGame() {
    if (gameRoom.value) setActiveRoom(gameRoom.value)
}

function isSelf(message: ChatMessage): boolean {
    return message.userId === localStore.user.id
}

function formatTime(value: string): string {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return ''
    return new Intl.DateTimeFormat(locale.value, { hour: '2-digit', minute: '2-digit' }).format(date)
}

function send() {
    if (!canSend.value) return
    sendChat(draft.value)
    draft.value = ''
    nextTick(() => scrollToBottom(true))
}

function onKeydown(event: KeyboardEvent) {
    if (event.key === 'Escape') {
        closeChat()
        return
    }
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault()
        send()
    }
}

function onScroll() {
    const element = listEl.value
    if (!element) return
    atBottom.value = element.scrollHeight - element.scrollTop - element.clientHeight < 40
}

function scrollToBottom(force = false) {
    const element = listEl.value
    if (!element) return
    if (!force && !atBottom.value) return
    element.scrollTop = element.scrollHeight
    atBottom.value = true
}

watch(() => chatStore.open, (open) => {
    if (!open) return
    selectDefaultRoom()
    nextTick(() => scrollToBottom(true))
})

watch(gameId, () => {
    if (chatStore.open) selectDefaultRoom()
})

watch(() => messages.value.length, () => {
    nextTick(() => { if (atBottom.value) scrollToBottom(true) })
})
</script>

<style scoped>
.chat-backdrop {
    position: fixed;
    inset: 0;
    z-index: 1000;
    display: flex;
    justify-content: flex-end;
    background-color: rgba(0, 0, 0, 0.28);
}

.chat-panel {
    display: flex;
    flex-direction: column;
    width: min(400px, 100%);
    height: 100%;
    background-color: #fff;
    box-shadow: -0.5rem 0 1.5rem rgba(0, 0, 0, 0.2);
}

.chat-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.9rem 1rem;
    border-bottom: 1px solid #e5e5e5;
}

.chat-title {
    font-size: 1.1rem;
    font-weight: 600;
}

.chat-close {
    border: 0;
    background: transparent;
    font-size: 1.5rem;
    line-height: 1;
    cursor: pointer;
}

.chat-tabs {
    display: flex;
    border-bottom: 1px solid #e5e5e5;
}

.chat-tabs button {
    flex: 1;
    padding: 0.6rem;
    border: 0;
    border-bottom: 2px solid transparent;
    background: transparent;
    color: #666;
    cursor: pointer;
}

.chat-tabs button.active {
    border-bottom-color: var(--main-color);
    color: var(--main-color);
    font-weight: 600;
}

.chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 0.75rem;
}

.chat-load-older {
    display: block;
    width: 100%;
    margin-bottom: 0.5rem;
    border: 0;
    background: transparent;
    color: var(--main-color);
    cursor: pointer;
}

.chat-empty {
    padding: 1rem;
    color: #888;
    text-align: center;
}

.chat-message {
    display: flex;
    gap: 0.5rem;
    margin-bottom: 0.75rem;
}

.chat-message.self {
    flex-direction: row-reverse;
}

.chat-message.pending {
    opacity: 0.6;
}

.chat-avatar {
    flex: 0 0 auto;
    width: 1.5rem;
    height: 1.5rem;
    margin-top: 0.15rem;
    border-radius: 50%;
}

.chat-content {
    min-width: 0;
}

.chat-message.self .chat-content {
    text-align: right;
}

.chat-meta {
    display: flex;
    gap: 0.5rem;
    align-items: baseline;
    color: #777;
    font-size: 0.75rem;
}

.chat-message.self .chat-meta {
    justify-content: flex-end;
}

.chat-name {
    font-weight: 600;
    color: #444;
}

.chat-text {
    margin-top: 0.1rem;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}

.chat-system {
    margin: 0.5rem 0;
    color: #888;
    font-size: 0.8rem;
    text-align: center;
}

.chat-error {
    margin: 0 0.75rem 0.5rem;
    color: #c62828;
    font-size: 0.8rem;
}

.chat-compose {
    display: flex;
    gap: 0.5rem;
    padding: 0.75rem;
    border-top: 1px solid #e5e5e5;
}

.chat-input {
    flex: 1;
    min-height: 2.5rem;
    max-height: 7rem;
    padding: 0.5rem;
    resize: vertical;
    border: 1px solid #ccc;
    border-radius: 0.3rem;
    font: inherit;
}

.chat-send {
    align-self: flex-end;
    padding: 0.5rem 0.9rem;
    border: 0;
    border-radius: 0.3rem;
    background-color: var(--main-color);
    color: #fff;
    cursor: pointer;
}

.chat-send:disabled {
    opacity: 0.5;
    cursor: default;
}

.chat-counter {
    padding: 0 0.75rem 0.5rem;
    color: #888;
    font-size: 0.75rem;
    text-align: right;
}

@media (max-width: 700px) {
    .chat-panel {
        width: 100%;
    }
}
</style>
