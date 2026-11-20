import { v4 as uuidv4 } from 'uuid'
import type { ChatMessage } from 'boardgame-web-common/back'
import { useLocalStore } from './localStore'
import { LOBBY_ROOM_ID, gameRoomId, useChatStore } from './chatStore'
import { wsService } from './wsService'

export { LOBBY_ROOM_ID, gameRoomId }

export function toggleChat() {
    const store = useChatStore()
    if (store.open) {
        store.close()
        return
    }
    store.openRoom(store.activeRoomId || LOBBY_ROOM_ID)
    loadRoom(store.activeRoomId || LOBBY_ROOM_ID)
}

export function closeChat() {
    useChatStore().close()
}

export function setActiveRoom(roomId: string) {
    const store = useChatStore()
    store.openRoom(roomId)
    loadRoom(roomId)
}

export function loadRoom(roomId: string) {
    const store = useChatStore()
    const room = store.ensureRoom(roomId)
    if (room.loaded || room.loading) return
    room.loading = true
    wsService.requestChatHistory(roomId)
}

export function loadOlder(roomId: string) {
    const store = useChatStore()
    const room = store.ensureRoom(roomId)
    if (!room.hasMore || room.loading) return
    const beforeSeq = room.messages[0]?.seq
    if (beforeSeq === undefined) return
    room.loading = true
    wsService.requestChatHistory(roomId, beforeSeq)
}

export function sendChat(text: string) {
    const trimmed = text.trim()
    if (!trimmed) return

    const store = useChatStore()
    const localStore = useLocalStore()
    const roomId = store.activeRoomId
    const requestId = uuidv4()
    const clientMessageId = uuidv4()

    const optimistic: ChatMessage = {
        id: `pending:${clientMessageId}`,
        roomId,
        seq: Number.MAX_SAFE_INTEGER,
        userId: localStore.user.id,
        userName: localStore.user.name,
        userColor: localStore.user.color,
        text: trimmed,
        createdAt: new Date().toISOString(),
        kind: 'USER',
        clientMessageId,
        deleted: false
    }

    store.addOptimistic(roomId, optimistic, requestId)
    wsService.sendChat(requestId, roomId, trimmed, clientMessageId)
}
