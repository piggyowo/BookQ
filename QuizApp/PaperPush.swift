import SwiftUI

/// 全局纸张推页容器。
/// 底栏页面把要进入的二级页放进 `detail`，只维护一个可选路由即可；
/// 进入/返回、根页退让和左缘拖回都由这一层统一处理。
///
/// 返回手感按 iOS 主流导航的交互式 pop 来做：
/// - 左缘起手，屏幕坐标 1:1 跟手，不挂在移动中的详情页上；
/// - 松手时同时看拖动距离和甩动速度，决定完成还是弹回；
/// - 完成动画按剩余距离缩放时长，拖得越远收得越快；
/// - 取消用短弹簧，不直接跳回。
struct PaperPushModifier<Detail: View>: ViewModifier {
    @Binding var isPresented: Bool
    let detail: () -> Detail

    @State private var isDetailVisible = false
    @State private var progress: CGFloat = 0
    @State private var dragOffset: CGFloat = 0
    @State private var isDraggingBack = false
    @State private var isClosing = false
    @State private var containerWidth: CGFloat = 1

    private let enterDuration: Double = 0.40
    private let exitDuration: Double = 0.48
    private let cancelResponse: Double = 0.38
    private let edgeStartWidth: CGFloat = 22

    func body(content: Content) -> some View {
        GeometryReader { geo in
            let w = max(geo.size.width, 1)
            let dragFraction = min(max(dragOffset / w, 0), 1)
            let effective = progress * (1 - dragFraction)

            ZStack {
                PaperBackground()

                content
                    .scaleEffect(1 - 0.040 * effective, anchor: .leading)
                    .offset(x: -w * 0.18 * effective)
                    .opacity(1 - Double(0.11 * effective))
                    .overlay {
                        Color.black
                            .opacity(Double(0.11 * effective))
                            .ignoresSafeArea()
                            .allowsHitTesting(false)
                    }
                    .allowsHitTesting(!isDetailVisible)

                if isDetailVisible {
                    detail()
                        .frame(width: geo.size.width, height: geo.size.height)
                        .offset(x: (1 - progress) * w + dragOffset)
                        .shadow(color: Color.black.opacity(0.14), radius: 10, x: -4, y: 0)
                        .overlay(alignment: .leading) {
                            Rectangle()
                                .fill(Palette.gold.opacity(0.18))
                                .frame(width: 1)
                                .allowsHitTesting(false)
                        }
                        // 手势必须挂在详情页上，但坐标要用屏幕坐标。
                        // 如果继续用 .local，详情页被 offset 后坐标系跟着移动，
                        // 手指不动页面在动，就会产生来回拉扯的卡顿。
                        .simultaneousGesture(backDrag(width: w))
                        .zIndex(2)
                }
            }
            .onAppear {
                containerWidth = w
                if isPresented && !isDetailVisible {
                    isDetailVisible = true
                    progress = 1
                }
            }
            .onChange(of: w) { newWidth in
                containerWidth = newWidth
            }
        }
        .onChange(of: isPresented) { presented in
            if presented {
                present()
            } else {
                close()
            }
        }
    }

    private func present() {
        if !isDetailVisible {
            isDetailVisible = true
            progress = 0
        }
        isClosing = false
        isDraggingBack = false
        dragOffset = 0
        DispatchQueue.main.async {
            withAnimation(.timingCurve(0.18, 0.84, 0.16, 1, duration: enterDuration)) {
                progress = 1
            }
        }
    }

    private func close() {
        guard isDetailVisible else { return }
        isClosing = true

        let width = max(containerWidth, 1)
        // 交互式 pop 的完成时长跟剩余距离相关：已经拖到很右边时，
        // 不应该再从很慢的速度走完全程。
        let remaining = min(max((width - dragOffset) / width, 0.16), 1)
        let duration = max(0.24, exitDuration * Double(remaining))

        withAnimation(.timingCurve(0.32, 0.70, 0.10, 1, duration: duration)) {
            progress = 0
            dragOffset = 0
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + duration + 0.05) {
            guard !isPresented else { return }
            isDetailVisible = false
            isClosing = false
            isDraggingBack = false
            progress = 0
            dragOffset = 0
        }
    }

    private func backDrag(width: CGFloat) -> some Gesture {
        DragGesture(minimumDistance: 0, coordinateSpace: .global)
            .onChanged { value in
                guard progress > 0.98, !isClosing else { return }

                let dx = value.translation.width
                let dy = value.translation.height

                if !isDraggingBack {
                    // 只允许左缘起手，避免和页面里的横向控件抢手势。
                    guard value.startLocation.x <= edgeStartWidth,
                          dx > 4,
                          abs(dx) > abs(dy) + 2 else { return }
                    isDraggingBack = true
                }

                guard isDraggingBack else { return }
                dragOffset = min(max(dx, 0), width)
            }
            .onEnded { value in
                guard isDraggingBack else { return }
                isDraggingBack = false

                let dx = min(max(value.translation.width, 0), width)
                let projected = value.predictedEndTranslation.width

                if dx > width * 0.30 || projected > width * 0.55 {
                    isClosing = true
                    isPresented = false
                } else {
                    withAnimation(
                        .spring(response: cancelResponse, dampingFraction: 0.88, blendDuration: 0.1)
                    ) {
                        dragOffset = 0
                    }
                }
            }
    }
}

extension View {
    func paperPush<Detail: View>(
        isPresented: Binding<Bool>,
        @ViewBuilder detail: @escaping () -> Detail
    ) -> some View {
        modifier(PaperPushModifier(isPresented: isPresented, detail: detail))
    }
}
