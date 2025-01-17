package org.wordpress.android.ui.mysite.cards.dynamiccard

import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.DynamicCardsModel.CardOrder
import org.wordpress.android.ui.deeplinks.handlers.DeepLinkHandlers
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.Dynamic
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DynamicCardsBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction.Companion.create
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.UrlUtilsWrapper
import javax.inject.Inject

class DynamicCardsBuilder @Inject constructor(
    private val urlUtils: UrlUtilsWrapper,
    private val deepLinkHandlers: DeepLinkHandlers
) {
    fun build(params: DynamicCardsBuilderParams, order: CardOrder): List<Dynamic>? {
        if (!shouldBuildCard(params, order)) {
            return null
        }
        return convertToDynamicCards(params, order)
    }

    private fun shouldBuildCard(params: DynamicCardsBuilderParams, order: CardOrder): Boolean {
        if (params.dynamicCards == null || params.dynamicCards.pages.none { it.order == order }) return false
        return true
    }

    private fun convertToDynamicCards(params: DynamicCardsBuilderParams, order: CardOrder): List<Dynamic> {
        val cards = params.dynamicCards?.pages?.filter { it.order == order } ?: emptyList()
        return cards.map { card ->
            Dynamic(
                id = card.id,
                rows = card.rows.map { row ->
                    Dynamic.Row(
                        iconUrl = row.icon,
                        title = row.title,
                        description = row.description,
                    )
                },
                title = card.title,
                image = card.featuredImage,
                action = getActionSource(params, card),
                onHideMenuItemClick = create(card.id, params.onHideMenuItemClick),
            )
        }
    }

    private fun getActionSource(
        params: DynamicCardsBuilderParams,
        card: CardModel.DynamicCardsModel.DynamicCardModel
    ): Dynamic.ActionSource? = when {
        isValidUrlOrDeeplink(card.url) && isValidActionTitle(card.action) -> Dynamic.ActionSource.Button(
            requireNotNull(card.url),
            create(requireNotNull(card.url), params.onActionClick),
            requireNotNull(card.action)
        )
        isValidUrlOrDeeplink(card.url) -> Dynamic.ActionSource.Card(
            requireNotNull(card.url),
            create(requireNotNull(card.url), params.onActionClick)
        )
        else -> null
    }

    private fun isValidUrlOrDeeplink(url: String?): Boolean {
        return !url.isNullOrEmpty() && (urlUtils.isValidUrlAndHostNotNull(url)
                || deepLinkHandlers.buildNavigateAction(UriWrapper(url)) != null)
    }

    private fun isValidActionTitle(title: String?): Boolean {
        return !title.isNullOrEmpty()
    }
}
